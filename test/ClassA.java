
public class ClassA {

	private String state;
    private ClassC component;

    public ClassA( ClassC c)
    {
        component = c;
    }

    public String operation()
    {
    	state = component.operation() ;
        return changeState( state ) ;
    }

	private String changeState(String in) {
		return "<em>" + state + "</em>" ;
	}

}
