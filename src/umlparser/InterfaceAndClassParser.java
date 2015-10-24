package umlparser;

import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class InterfaceAndClassParser extends VoidVisitorAdapter<Object>{
	
	private boolean isAnInterface = false;
	private List<String> extended;
	private List<String> implemented;
	private String classOrInterfaceName;
	
	public InterfaceAndClassParser(){
		extended = new ArrayList<String>();
		implemented = new ArrayList<String>();
	}

	
	public void visit(ClassOrInterfaceDeclaration cid, Object arg) {
		
		if (cid.getName() == null) {
			throw new IllegalArgumentException("Illegal ClassOrInterfaceDeclaration argument in InterfaceAndClassParser");
		} else {
			this.classOrInterfaceName = cid.getName();
			List<ClassOrInterfaceType> lstImp = cid.getImplements();
			if (lstImp != null && !lstImp.isEmpty()){
				for (ClassOrInterfaceType cit : lstImp){
					implemented.add(cit.toString());
				}
			}

			
			List<ClassOrInterfaceType> lstExt = cid.getExtends();
			if (lstExt != null && !lstExt.isEmpty()){
				for (ClassOrInterfaceType cit : lstExt){
					extended.add(cit.toString());
				}

			}

			if(cid.isInterface()) {
				
				this.isAnInterface = true;
			}
		}
	}
	
	
	public boolean isInterface(){
		return this.isAnInterface;
	}
	
	public List<String> getExtends(){
		return this.extended;
		
	}
	
	public List<String> getImplements(){
		return this.implemented;
	}
	
	public String getClassOrInterfaceName() {
		return this.classOrInterfaceName;
	}

}
