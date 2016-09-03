package umlparser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sourceforge.plantuml.SourceStringReader;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.BlockStmt;

public class UmlParser {
	
	
	private List<String> classNames;  //class names used in the target package
	private List<String> interfaceNames;  //interface names used in the target package
	private Map<String, List<String>> implementedInterfaces;  //a map for the implemented interfaces of each class
	private Map<String, List<String>> extendedClasses; //a map for the extended interfaces of each class
	private Map<String, List<String>> attributeMap; //a map for the attributes(primitive variable instances) of each class
	private Map<String, List<String>> associateMap; //a map for the associates(classes in the target package) of each class
	private List<CompilationUnit> cuList; // list of compilation unit
	private Map<String,Map<String, List<String>>> methodsMap; //methods of each class
	private Map<String, Map<String, String>> methodReturnMap;  //the return type of each method of each class
	private Map<String, List<String>> uses;
	private Map<String, List<String>> constructsMap;
	private Map<String, List<String>> constructsDenpendencies;
	private Map<String, List<String>> mainbodyDependencies;
	private Map<String, Map<String,String>> multiplicityMapMap;
	private Set<String> ballSocketInterfaceNames;
	private Map<String, List<String>> lollipopInterfaceAndUsers;
	private String classWithMain = "";
	private Map<String,Map<String,List<SequenceDiagramActor>>> actorListMap;
	
	private List<String> sequenceDiagramGrammer = new ArrayList<String>(); //list of grammer strings, will be joined as a single string at last
	
	
	public UmlParser(){

		classNames = new ArrayList<String>();
		interfaceNames = new ArrayList<String>();
		implementedInterfaces = new HashMap<String, List<String>>();
		extendedClasses = new HashMap<String, List<String>>();
		cuList = new ArrayList<CompilationUnit>();
		attributeMap = new HashMap<String, List<String>>();
		associateMap = new HashMap<String, List<String>>();
		methodsMap = new HashMap<String,Map<String, List<String>>>();
		methodReturnMap = new HashMap<String, Map<String, String>>();
		uses = new HashMap<String, List<String>>();
		constructsMap = new HashMap<String, List<String>>();
		constructsDenpendencies = new HashMap<String, List<String>>();
		mainbodyDependencies = new HashMap<String, List<String>>();
		multiplicityMapMap = new HashMap<String, Map<String,String>>();
		ballSocketInterfaceNames = new HashSet<String>();
		lollipopInterfaceAndUsers = new HashMap<String, List<String>>();
		actorListMap = new HashMap<String,Map<String,List<SequenceDiagramActor>>>();
		new HashMap<String, List<String>>();
	}
	
	@SuppressWarnings("unchecked")
	private void walkDir(String pathString) throws IOException {
		File folder = new File(pathString);
		if(folder.isDirectory()) {
			File[] files = folder.listFiles(new FilenameFilter(){

				@Override
				public boolean accept(File dir, String name) {
					String lowerCaseFileName = name.toLowerCase();
					return lowerCaseFileName.endsWith(".java");
				}
				
			});
			for (File file: files) {
				
				//obtain compilation unit objects
				FileInputStream in = new FileInputStream(file);
				CompilationUnit cu = null;
				
				try{
					cu = JavaParser.parse(in);
					cuList.add(cu);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					in.close();
				}
				
				
				//get interfaces implemented by current class
				InterfaceAndClassParser iacp = new InterfaceAndClassParser();
				iacp.visit(cu, null);
				
				String cuName = iacp.getClassOrInterfaceName();
				if(iacp.isInterface()) {
					
					this.interfaceNames.add(cuName);
					
				} else {
					this.classNames.add(cuName);
				}
				
				extendedClasses.put(cuName, iacp.getExtends());
				implementedInterfaces.put(cuName, iacp.getImplements());
				
				FieldParser fp = new FieldParser();
				fp.visit(cu);
				attributeMap.put(cuName, fp.getAttributes());
				associateMap.put(cuName,fp.getAssociates());
				multiplicityMapMap.put(cuName, fp.getMultiplicityMap());
				
				ConstructorParser cp = new ConstructorParser();
				cp.visit(cu, null);
				constructsMap.put(cuName, cp.getConstructs());
				constructsDenpendencies.put(cuName,new ArrayList(cp.getConstructDependencies()));
				
				MethodParser mp = new MethodParser();
				
				mp.visit(cu, fp.getObjectClassMap());
				methodsMap.put(cuName, mp.getMethodsMap());
				methodReturnMap.put(cuName, mp.getMethodReturnTypeMap());
				uses.put(cuName, new ArrayList(mp.getMethodDependencies()));
				mainbodyDependencies.put(cuName, new ArrayList(mp.getMainBodyDependencies()));
				actorListMap.put(cuName, mp.getActorListMap());
				if (mp.getIsClassWithMain()) {
					classWithMain = cuName;
				}
	
			}
		}
		
		classNames.addAll(interfaceNames);
		


		
	}
	
	
	public void generateClassDiagram(String outputPath){
		List<String> classDiagramGrammer = new ArrayList<String>(); //list of grammer strings, will be joined as a single string at last
		classDiagramGrammer.add("@startuml");
		classDiagramGrammer.add("skinparam classAttributeIconSize 0");
		List<String> strlist = new ArrayList<String>();
		for (String className : classNames) {
			StringBuilder sb = new StringBuilder();
			if(interfaceNames.contains(className)) {
				sb.append("interface " + className + "<<interface>> {\n");
			} else {
				sb.append("class " + className + " {\n");
			}
			
			for (String attr: attributeMap.get(className)){
				sb.append(attr+"\n");
			}
			
			for(String constructMethod: constructsMap.get(className)){
				sb.append(constructMethod.replace('[', '(').replace(']', ')')+"\n" );
				
			}
			
			//methodMap: Map<className, Map<methodName, parameterList>>
			for (Map.Entry<String, List<String>> method : methodsMap.get(className).entrySet()) {
				List<String> parameters = method.getValue();
				String returnType = methodReturnMap.get(className).get(method.getKey());
				
				//remove private methods, setters and getters
				if (method.getKey().startsWith("-")){
					continue;
				}
				
				boolean isSetterOrGetter = false;
				int setStartIndex = method.getKey().indexOf("set");
				int getStartIndex = method.getKey().indexOf("get");
				if (setStartIndex != -1) {
					String setterStringAttr = method.getKey().substring(setStartIndex+3).toLowerCase();

					List<String> attributeList = attributeMap.get(className);
					
					for (String attribStr: attributeList) {
						String attributeName = attribStr.split(":")[0].substring(1);
						if(setterStringAttr.indexOf(attributeName) != -1){
							isSetterOrGetter = true;
							break;
						}
					}
					
					List<String> associateList = associateMap.get(className);
					for (String associateStr : associateList) {
						String associateName = associateStr;//.split(":")[0].substring(1);
						if(setterStringAttr.indexOf(associateName) != -1){
							isSetterOrGetter = true;
							break;
						}
					}
				}
				if (getStartIndex != -1 && !isSetterOrGetter) {
					String getterStringAttr = method.getKey().substring(setStartIndex+3).toLowerCase();

					List<String> attributeList = attributeMap.get(className);
					
					for (String attribStr: attributeList) {
						String attributeName = attribStr.split(":")[0].substring(1);
						if(getterStringAttr.indexOf(attributeName) != -1){
							isSetterOrGetter = true;
							break;
						}
					}
					
					List<String> associateList = associateMap.get(className);
					for (String associateStr : associateList) {
						String associateName = associateStr;//.split(":")[0].substring(1);
						if(getterStringAttr.indexOf(associateName) != -1){
							isSetterOrGetter = true;
							break;
						}
					}
				}
				if(!isSetterOrGetter){
					sb.append(method.getKey() + parameters.toString().replace('[', '(').replace(']', ')') + ":" +returnType+"\n");
				}	
			}
			

			sb.append("\n}");
			strlist.add(sb.toString());
		}
		
		//draw relationships
		for (String className : classNames) {
			

			List<String> extClasses = extendedClasses.get(className);
			for (String ext: extClasses) {
				strlist.add(className + "--|>" + ext);
			}
			
			List<String> associates = associateMap.get(className);
			List<String> constructDepend = constructsDenpendencies.get(className);
			List<String> used = uses.get(className);

			List<String> implInterfaces = implementedInterfaces.get(className);
			for (String impl: implInterfaces) {
				strlist.add(className + "..|>" + impl);
			}
			
			
			List<String> mainbodyUseList = mainbodyDependencies.get(className);
			for (String mainbodyUse: mainbodyUseList) {
				if (interfaceNames.contains(mainbodyUse)){
					used.add(mainbodyUse);
				}
			}
			
			used.addAll(associates);
			used.addAll(constructDepend);
			for (String use	: used) {
				if (interfaceNames.contains(use) ) {
//					ball-and-socket
					
					if (implementedInterfaces.get(className).contains(use)){
						//record the interface that need to be shown as ball-n-socket
						if(lollipopInterfaceAndUsers.containsKey(use)) {
							lollipopInterfaceAndUsers.get(use).add(className);
						} else {
							lollipopInterfaceAndUsers.put(use, new ArrayList<String>());
							lollipopInterfaceAndUsers.get(use).add(className);
						}
						
						
						ballSocketInterfaceNames.add(use);
					} else {
						if (strlist.contains(use + "..>" + className)){
						strlist.remove(use + "..>" + className);
						strlist.add(className + ".." + use);
						} else {
						strlist.add(className + "..> " + use);
						}
					}
					
				}
			}
			
		}
		
		List<String> strsToRemove = new ArrayList<String>();
		List<String> strsToAdd = new ArrayList<String>();
		for(String str: strlist){
			if (str.indexOf("..|>") != -1){
				int index = str.indexOf("..|>");
				
				
//				System.out.println(str);
				String interf = str.substring(index+4,str.length());
				String extender = str.substring(0,index);
				if(lollipopInterfaceAndUsers.containsKey(interf)) {

					strsToRemove.add(str);
					//output = concreteClass + "-0)-" + client + ":\"" + interfaceName + "\"";
					for (String user: lollipopInterfaceAndUsers.get(interf)) {
						if (!extender.equals(user) ) {
							strsToAdd.add(extender + "-0)-" +user + ":\"" + interf + "\"" );
						}
						
					}
					

				}
			}
		}
		
		for (String lollipopInterface: lollipopInterfaceAndUsers.keySet()) {
			for (String str : strlist){
				if (str.indexOf("interface " + lollipopInterface) != -1){
					strsToRemove.add(str);
				}
			}

		}
		
		for (String strToRemove: strsToRemove) {
			strlist.remove(strToRemove);
		}
		
		for (String strToAdd: strsToAdd) {
			if(!strlist.contains(strToAdd)){
				strlist.add(strToAdd);
			}
			
		}
		
		for (String className :classNames) {
			List<String> associates = associateMap.get(className);
			for (String associate : associates){
				String multiplicityleft ="";
				if (multiplicityMapMap.get(associate) != null){
					if (multiplicityMapMap.get(associate).get(className) != null){
						multiplicityleft = multiplicityMapMap.get(associate).get(className);
					}
				}

				if (multiplicityleft.equals("")) {
					multiplicityleft = "\"1\"";
				} else {
					multiplicityleft = '"'+ multiplicityleft + '"';
				}
				String multiplicityRight = "";
				if (multiplicityMapMap.get(className) != null){
					if( multiplicityMapMap.get(className).get(associate) != null){
						multiplicityRight = multiplicityMapMap.get(className).get(associate);
					}
				}
				
				
				if (multiplicityRight.equals("")) {
					multiplicityRight = "\"1\"";
				} else {
					multiplicityRight = '"'+ multiplicityRight + '"';
				}
				
				String mulitLeftStr = className +multiplicityleft + "--"+ multiplicityRight + associate;
				String mulitRightStr = associate + multiplicityRight+"--"+ multiplicityleft + className;
				
				if (!strlist.contains(mulitLeftStr) && !strlist.contains(mulitRightStr) 
						&& classNames.contains(associate)){
					strlist.add(mulitLeftStr);
				}
			}
		}
		

		
		
		classDiagramGrammer.addAll(strlist);
		classDiagramGrammer.add("@enduml");
		
		File file = new File(outputPath + "ClassDiagram.png");
		OutputStream png = null;
		try {
			png = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		SourceStringReader reader = new SourceStringReader(String.join("\n", classDiagramGrammer));
		// Write the first image to "png"
		String desc = null;
		try {
			desc = reader.generateImage(png);
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		
	}

	
	public void generateSequenceDiagram(String outputPath) {
		if (classWithMain == "") {
			return;
		}
		
		sequenceDiagramGrammer.add("@startuml\n");
		dfs(classWithMain, "main");
		sequenceDiagramGrammer.add("@enduml\n");
		
		File file = new File(outputPath + "SequenceDiagram.png");
		OutputStream png = null;
		try {
			png = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		SourceStringReader reader = new SourceStringReader(String.join("\n", sequenceDiagramGrammer));
		// Write the first image to "png"
		String desc = null;
		try {
			desc = reader.generateImage(png);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void dfs(String className, String methodName){
		if (className == null || methodName == null) {
			return;
		}
		

		
		for (SequenceDiagramActor actor : actorListMap.get(className).get(methodName)) {

			if (actor.getClassName() != null && actor.getMethodName() != null){
				sequenceDiagramGrammer.add(className + " -> " + actor.getClassName() + ":" + actor.getMethodName() + "\n");
				sequenceDiagramGrammer.add("activate " + actor.getClassName() + "\n" );
				dfs(actor.getClassName(),actor.getMethodName());
				if (!className.equals(actor.getClassName())) {
					sequenceDiagramGrammer.add(actor.getClassName() + " --> " + className  + "\n");
				}
				sequenceDiagramGrammer.add("deactivate " + actor.getClassName() + "\n" );
			}
		}
		
	}
	
	
	public static void main(String[] args) {
		File folder = null;
		String outputPath = "";
		
		if (args.length > 1 || args.length == 0) {
			usage();
			System.exit(1);
		} else {
			folder = new File(args[0]);
			if (!folder.isDirectory()) {
				usage();
				System.exit(1);
			}
			if(folder.listFiles().length == 0) {
				System.out.println("Empty directory! Please indicate where the java files are!");
			}
			
			outputPath = args[0];
		
		}
		
		if(outputPath != "") {
			UmlParser umlparser = new UmlParser();
			try {
				umlparser.walkDir(outputPath);
				umlparser.generateClassDiagram(outputPath);
//				umlparser.generateSequenceDiagram(outputPath);
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
	}
		
		private static void usage() {
			System.out.println("Usage: umlparser <classpath>");
			System.out.println("<classpath> is a folder name where all the .java source files are.");
			System.out.println("The output file will be at current folder.");
		}
	
}
