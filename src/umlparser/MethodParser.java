package umlparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class MethodParser extends VoidVisitorAdapter {
	
	public static final ArrayList<String> reservedTypes = new ArrayList<String>(Arrays.asList("byte","short","int","long","float","double","boolean","char","Integer","String", "Character"));
	
	private Map<String, List<String>> methodMap;// = new HashMap<String, List<String>>(); 
	private Map<String, String> methodReturnTypeMap;// = new HashMap<String, String>(); 
	private Set<String> methodDependencies;
	private Set<String> mainBodyDependencies;
	
	public MethodParser() {
		methodMap = new HashMap<String, List<String>>(); 
		methodReturnTypeMap = new HashMap<String, String>();
		methodDependencies = new HashSet<String>();
		mainBodyDependencies = new HashSet<String>();
	}
	
	

	public void visit(MethodDeclaration md, Object arg) {
//		System.out.println(md.getTypes().size());
//		System.out.println(md.getTypes().get(0).getMembers());
		
		
//		System.out.println(md.getModifiers());
//		System.out.println(md.getParameters());
		
		List<String> revisedParaStrings = new ArrayList<String>();
		for (Parameter parameter : md.getParameters()){
			String[] parameterStrings = parameter.toString().split(",");
			for (String eachPara: parameterStrings) {
				String[] eachParaParts = eachPara.split(" ");
//				for (String reservedType: reservedTypes) {
//					if(eachParaParts[0].indexOf(reservedType) != -1) {
//						continue;
//					} else {
						methodDependencies.add(eachParaParts[0]);
//					}
//				}

				revisedParaStrings.add(eachParaParts[1] + ":" + (eachParaParts[0]== null? "void":eachParaParts[0]));
//				System.out.println(revisedParaStrings);

			}
		}
		String modifier = null;
//		System.out.println(md.getModifiers());
		switch(md.getModifiers()){
		case 0: modifier = "~";
			break;
		case 1: modifier = "+";
			break;
		case 2: modifier = "-";
			break;
		case 4: modifier = "#";
			break;
		}
//		System.out.println(modifier);
		if (md.getName().equals("main")) {
			methodMap.put("+main", revisedParaStrings);
			
			methodReturnTypeMap.put("+main", "void");
		} else {
			methodMap.put((modifier==null?"+":modifier) + md.getName(), revisedParaStrings);
			
			methodReturnTypeMap.put(modifier + md.getName(), (md.getType()==null?"void":md.getType().toString()));
		}

		
//		System.out.println(md.getDeclarationAsString(false, false));
//		System.out.println(md.getType());
		
		if(md.getName().equals("main")){
			String statements = md.getBody().getStmts().get(0).toString();
			String[] lines = statements.split(";");
			for(String line: lines){
				String[] segments = line.split(" ");
				mainBodyDependencies.add(segments[0]);
			}
		}
		
		
	}
	
	public Map<String, List<String>> getMethodsMap(){
//		System.out.println(methodMap);
		return methodMap;
	}
	
	public Map<String, String> getMethodReturnTypeMap(){
		return methodReturnTypeMap;
	}
	
	public Set<String> getMethodDependencies(){
		for (String depend: this.methodDependencies) {
			for (String type: reservedTypes) {
				if (depend.indexOf(type) != -1) {
					this.methodDependencies.remove(depend);
				}
			}
		}
		return methodDependencies;
	}
	
	public Set<String> getMainBodyDependencies(){
		return mainBodyDependencies;
	}

}
