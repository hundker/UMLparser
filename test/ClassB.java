
public class ClassB {

    private ClassA component;
    private String state;

    public ClassB( ClassA c)
    {
        component = c;
    }

    public String operation()
    {
        state = component.operation() ;
        return changeState( state ) ;
    }

    private String changeState(String in) {
        return "<h1>" + state + "</h1>" ;
    }

}
