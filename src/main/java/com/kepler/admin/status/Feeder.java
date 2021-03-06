package com.kepler.admin.status;

import java.util.Map;

import com.kepler.annotation.Async;
import com.kepler.annotation.Internal;
import com.kepler.annotation.Service;
import com.kepler.host.Host;

/**
 * @author kim 2015年8月8日
 */
@Service(version = "0.0.8")
@Internal
public interface Feeder {

	/**
	 * @param timestmap 传送时间
	 * @param local 主机
	 * @param status 状态集合
	 */
	@Async
	public void feed(long timestamp, Host local, Map<String, Point> status);
}
