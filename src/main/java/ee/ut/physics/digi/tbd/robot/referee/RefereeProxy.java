package ee.ut.physics.digi.tbd.robot.referee;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

public class RefereeProxy implements Referee {

    private Referee concreteReferee;

    private final Set<RefereeListener> listeners = new HashSet<>();
    private final RefereeListener concreteListener;

    public RefereeProxy() {
        concreteListener = (RefereeListener) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                                                    new Class[] { RefereeListener.class },
                                                                    this.new RefereeListenerInvocationHandler());
    }

    @Override
    public void registerListener(RefereeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(RefereeListener listener) {
        listeners.remove(listener);
    }

    public void setConcreteReferee(Referee concreteReferee) {
        if(this.concreteListener != null) {
            concreteReferee.unregisterListener(concreteListener);
        }
        this.concreteReferee = concreteReferee;
        if(concreteListener != null) {
            concreteReferee.registerListener(concreteListener);
        }
    }

    private class RefereeListenerInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            for(RefereeListener listener : listeners) {
                method.invoke(listener, args);
            }
            return null;
        }

    }

}
