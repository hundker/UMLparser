package umlparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class MethodParser extends VoidVisitorAdapter {
	
	public static final ArrayList<String> reservedTypes = new ArrayList<String>(Arrays.asList("byte","short","int","long","float","double","boolean","char","Integer","String", "Character"));
	
	private Map<String, List<String>> methodMap;//  
	private Map<String, String> methodReturnTypeMap;// 
	private Set<String> methodDependencies;
	private Set<String> mainBodyDependencies;
	private boolean isClassWithMain;
	private Map<String, String> objectClassMap;
	private Map<String, List<SequenceDiagramActor>> actorListMap;
	private Set<String> methodNameSet;
	
	public MethodParser() {
		methodMap = new HashMap<String, List<String>>(); 
		methodReturnTypeMap = new HashMap<String, String>();
		methodDependencies = new HashSet<String>();
		mainBodyDependencies = new HashSet<String>();
		isClassWithMain = false;
		objectClassMap = new HashMap<String, String>();
		actorListMap = new HashMap<String, List<SequenceDiagramActor>>();
		methodNameSet = new HashSet<String>();
	}
	
	

	public void visit(MethodDeclaration md, Object arg) {


		
		List<String> revisedParaStrings = new ArrayList<String>();
		List<SequenceDiagramActor> actorList = new ArrayList<SequenceDiagramActor>();
		methodNameSet.add(md.getNameExpr().toString());
		for (Parameter parameter : md.getParameters()){
			String[] parameterStrings = parameter.toString().split(",");
			for (String eachPara: parameterStrings) {
				String[] eachParaParts = eachPara.split(" ");

				methodDependencies.add(eachParaParts[0]);

				if (Pattern.matches("[A-Za-z]+", eachParaParts[0]) && !reservedTypes.contains(eachParaParts[0])){
					objectClassMap.put(eachParaParts[1], eachParaParts[0]);
				}
				
				revisedParaStrings.add(eachParaParts[1] + ":" + (eachParaParts[0]== null? "void":eachParaParts[0]));


			}
		}
		String modifier = null;

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

		if (md.getName().equals("main")) {
			methodMap.put("+main", revisedParaStrings);
			
			methodReturnTypeMap.put("+main", "void");
			isClassWithMain = true;
		} else {
			methodMap.put((modifier==null?"+":modifier) + md.getName(), revisedParaStrings);
			
			methodReturnTypeMap.put(modifier + md.getName(), (md.getType()==null?"void":md.getType().toString()));
		}


		
		if(md.getName().equals("main")){
			String statements = md.getBody().getStmts().get(0).toString();
			String[] lines = statements.split(";");
			for(String line: lines){
				String[] segments = line.split(" ");
				mainBodyDependencies.add(segments[0]);
			}
		}
		String thisClassName = md.getParentNode().toStringWithoutComments().split(" ")[2];
		if (md.getBody() != null) {
			List<Statement> stmtList = md.getBody().getStmts();
			if(stmtList != null) {

				for (Statement stmt : stmtList){
					MethodCallExpr methodCallExpr = findMethodCallExpr(stmt);

					if (methodCallExpr != null) {

						Expression scopeExp = methodCallExpr.getScope();

						String scopeString ="";
						if (scopeExp != null) {
							scopeString = scopeExp.toStringWithoutComments();
						} else {
							scopeString = "self";
						}
						SequenceDiagramActor actor = new SequenceDiagramActor();

						actor.setObjectName(scopeString);
						actor.setMethodName(methodCallExpr.getName());
						actor.setMethodCallList(new HashMap<String, List<String>>() {
							{
								put(methodCallExpr.getName(),
										new ArrayList<String>());
							}
						});

						actorList.add(actor);


					} else {
						String[] objects = stmt.toStringWithoutComments().split(" ");
						if(objects != null && objects.length >= 2 ) {
							if (Pattern.matches("[A-Za-z]+", objects[1]) && !reservedTypes.contains(objects[0])) {
								objectClassMap.put(objects[1], objects[0]);
							}

						}
					}
				}
			}
		}
		
		Map<String, String> objectMap = (HashMap<String,String>)arg;
		for (SequenceDiagramActor actor : actorList){
			if (objectClassMap.get(actor.getObjectName()) != null){
				actor.setClassName(objectClassMap.get(actor.getObjectName()));
				
			}
			
			if (objectMap.get(actor.getObjectName()) != null) {
				actor.setClassName(objectMap.get(actor.getObjectName()));
			}
			
			if (actor.getObjectName().equals("self")){
				actor.setClassName(thisClassName);
			}
			

		}
		

		actorListMap.put(md.getName(), actorList);

	}
	
    private MethodCallExpr findMethodCallExpr(Node node) {
        if (node instanceof MethodCallExpr) {
            return (MethodCallExpr)node;
        }
        List<Node> nodes = node.getChildrenNodes();
        if (nodes != null) {
            for (Node nd : nodes) {
                MethodCallExpr next = findMethodCallExpr(nd);
                if (next != null) {
                    return next;
                }
            }
        }
        return null;
    }
	
	public Map<String, List<String>> getMethodsMap(){

		return methodMap;
	}
	
	public Map<String, String> getMethodReturnTypeMap(){
		return methodReturnTypeMap;
	}
	
	public Set<String> getMethodDependencies(){
		
		List<String> toRemove = new ArrayList<String>();
		for (String depend: this.methodDependencies) {
			for (String type: reservedTypes) {
				if (depend.indexOf(type) != -1) {
					toRemove.add(depend);
				}
			}
		}
		
		for (String todel : toRemove){
			this.methodDependencies.remove(todel);
		}
		
		
		return this.methodDependencies;
	}
	
	public Set<String> getMainBodyDependencies(){
		return mainBodyDependencies;
	}
	
	public boolean getIsClassWithMain() {
		return isClassWithMain;
	}
	
	public Map<String, List<SequenceDiagramActor>> getActorListMap(){
		return actorListMap;
	}

}
