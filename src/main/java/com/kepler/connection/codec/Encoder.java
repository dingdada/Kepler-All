package com.kepler.connection.codec;

import java.io.OutputStream;

import com.kepler.config.PropertiesUtils;
import com.kepler.serial.SerialID;
import com.kepler.serial.Serials;
import com.kepler.traffic.Traffic;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator.Handle;
import io.netty.util.ReferenceCountUtil;

/**
 * @author KimShen
 *
 */
/**
 * @author KimShen
 *
 */
public class Encoder {

	/**
	 * 调整因子
	 */
	private static final double ADJUST = PropertiesUtils.get(Encoder.class.getName().toLowerCase() + ".adjust", 0.75);

	/**
	 * 是否使用内存池分配
	 */
	private static final boolean POOLED = PropertiesUtils.get(Encoder.class.getName().toLowerCase() + ".pooled", true);

	/**
	 * 是否预估计算分配大小
	 */
	private static final boolean ESTIMATE = PropertiesUtils.get(Encoder.class.getName().toLowerCase() + ".estimate", true);

	/**
	 * 分配计算
	 */
	private final Handle estimate = AdaptiveRecvByteBufAllocator.DEFAULT.newHandle();

	/**
	 * 内存分配
	 */
	private final ByteBufAllocator allocator = Encoder.POOLED ? PooledByteBufAllocator.DEFAULT : UnpooledByteBufAllocator.DEFAULT;

	private final Serials serials;

	private final Traffic traffic;

	private final Class<?> clazz;

	public Encoder(Traffic traffic, Serials serials, Class<?> clazz) {
		super();
		this.clazz = clazz;
		this.serials = serials;
		this.traffic = traffic;
	}

	public ByteBuf encode(Object message) throws Exception {
		// 分配ByteBuf
		ByteBuf buffer = Encoder.ESTIMATE ? this.estimate.allocate(this.allocator) : this.allocator.ioBuffer();
		try {
			// 获取序列化策略(如Request/Response)
			byte serial = SerialID.class.cast(message).serial();
			// 首字节为序列化策略
			return BufferOutputStream.class.cast(this.serials.output(serial).output(message, this.clazz, new BufferOutputStream(buffer.writeByte(serial)), (int) (buffer.capacity() * Encoder.ADJUST))).record(this.traffic, this.estimate).buffer();
		} catch (Exception exception) {
			// 异常, 释放ByteBuf
			if (buffer.refCnt() > 0) {
				ReferenceCountUtil.release(buffer);
			}
			throw exception;
		}
	}

	private class BufferOutputStream extends OutputStream {

		private ByteBuf buffer;

		private BufferOutputStream(ByteBuf buffer) {
			this.buffer = buffer;
		}

		/**
		 * 安全性校验
		 * 
		 * @param dest
		 * @param offset
		 * @param length
		 */
		private void valid(byte[] dest, int offset, int length) {
			if (dest == null) {
				throw new NullPointerException();
			} else if ((offset < 0) || (offset > dest.length) || (length < 0) || ((offset + length) > dest.length) || ((offset + length) < 0)) {
				throw new IndexOutOfBoundsException();
			}
		}

		public ByteBuf buffer() {
			return this.buffer;
		}

		@Override
		public void write(int data) {
			this.buffer.writeByte(data);
		}

		public void write(byte[] src) {
			this.write(src, 0, src.length);
		}

		public void write(byte[] src, int offset, int length) {
			this.valid(src, offset, length);
			if (length == 0) {
				return;
			}
			this.buffer.writeBytes(src, offset, length);
		}

		/**
		 * 写入完毕, 回调写入信息
		 * 
		 * @param traffic
		 * @param estimate
		 * @return
		 */
		public BufferOutputStream record(Traffic traffic, Handle estimate) {
			int readable = this.buffer.readableBytes();
			// 流量记录
			traffic.output(readable);
			// 预估大小更新(如果开启了预估)
			if (Encoder.ESTIMATE) {
				estimate.record(readable);
			}
			return this;
		}
	}
}
