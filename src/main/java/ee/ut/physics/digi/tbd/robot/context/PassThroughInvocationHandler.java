package ee.ut.physics.digi.tbd.robot.context;

import lombok.Setter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class PassThroughInvocationHandler<T> implements InvocationHandler {

    @Setter
    private T target;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(target, args);
    }

}
