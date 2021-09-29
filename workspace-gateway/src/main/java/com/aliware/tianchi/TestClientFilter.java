package com.aliware.tianchi;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.remoting.TimeoutException;
import org.apache.dubbo.rpc.*;

/**
 * 客户端过滤器（选址后）
 * 可选接口
 * 此类可以修改实现，不可以移动类或者修改包名
 * 用户可以在客户端拦截请求和响应,捕获 rpc 调用时产生、服务端返回的已知异常。
 */
@Activate(group = CommonConstants.CONSUMER)
public class TestClientFilter implements Filter, BaseFilter.Listener {


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        NodeManager.state(invoker).active.getAndIncrement();
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        NodeState state = NodeManager.state(invoker);
        String value = appResponse.getAttachment("w");
        if (null != value) {
            state.setServerActive(Long.parseLong(value));
        }
        //仅仅记录超时的 -- 乘以weight
        NodeManager.state(invoker).active.getAndDecrement();
        NodeManager.state(invoker).end(appResponse.hasException() &&
                appResponse.getException() instanceof TimeoutException);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
    }
}
