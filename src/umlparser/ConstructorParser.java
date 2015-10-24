package umlparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class ConstructorParser extends VoidVisitorAdapter{
	
	public static final ArrayList<String> reservedTypes = new ArrayList<String>(Arrays.asList("byte","short","int","long","float","double","boolean","char","Integer","String", "Character"));
	private List<String> constructs;
	private Set<String> constructDependencies;
	
	
	public ConstructorParser(){
		constructs = new ArrayList<String>();
		constructDependencies = new HashSet<String>();
	}
	
	public void visit(ConstructorDeclaration cd, Object arg) {
//		System.out.println(cd);
		String modifier = null;
		switch (cd.getModifiers()) {
		case 0:
			modifier = "~";
			break;
		case 1:
			modifier = "+";
			break;
		case 2:
			modifier = "-";
			break;
		case 4:
			modifier = "#";
			break;
		}
		
		List<String> revisedParaStrings = new ArrayList<String>();
		for (Parameter parameter : cd.getParameters()){
			String[] parameterStrings = parameter.toString().split(",");
			for (String eachPara: parameterStrings) {
				String[] eachParaParts = eachPara.split(" ");
//				for (String reservedType: reservedTypes) {
//					if(eachParaParts[0].indexOf(reservedType) != -1) {
//						continue;
//					} else {
						constructDependencies.add(eachParaParts[0]);
//					}
//				}

				revisedParaStrings.add(eachParaParts[1] + ":" + eachParaParts[0]);
			}
		}
		String constructParameterList = String.join(",", revisedParaStrings);
		constructs.add(modifier + cd.getName() + "("+ constructParameterList + ")");
		
	}
	
	public List<String> getConstructs(){
		return this.constructs;
	}
	
	public Set<String> getConstructDependencies(){
		for (String depend: this.constructDependencies) {
			for (String type: reservedTypes) {
				if (depend.indexOf(type) != -1) {
					this.constructDependencies.remove(depend);
				}
			}
		}
		return this.constructDependencies;
	}

}
