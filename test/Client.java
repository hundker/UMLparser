
public class Client {

    public static void main(String[] args)
    {
        ClassB obj = new ClassB( new ClassA( new ClassC() ) ) ;
        String result = obj.operation() ;
        System.out.println(result);
    }
}
