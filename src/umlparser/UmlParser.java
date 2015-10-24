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

public class UmlParser {
	
	private List<String> plantGrammer; //list of grammer strings, will be joined as a single string at last
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
	private Map<String, List<String>> lollipopInterfaceAndExtenders;
	
	public UmlParser(){
		plantGrammer = new ArrayList<String>();
		plantGrammer.add("@startuml");
		plantGrammer.add("skinparam classAttributeIconSize 0");
		
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
		lollipopInterfaceAndExtenders = new HashMap<String, List<String>>();
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
				
//				System.out.println(cu.toString());
				
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
				
				mp.visit(cu, null);
				methodsMap.put(cuName, mp.getMethodsMap());
				methodReturnMap.put(cuName, mp.getMethodReturnTypeMap());
				uses.put(cuName, new ArrayList(mp.getMethodDependencies()));
				mainbodyDependencies.put(cuName, new ArrayList(mp.getMainBodyDependencies()));
//				System.out.println(mp.getMethodsMap());
				

				
			}
		}
		
		classNames.addAll(interfaceNames);
		


		
	}
	
	
	public void generateUML(String path){
		List<String> strlist = new ArrayList<String>();
		for (String className : classNames) {
			if(interfaceNames.contains(className)) {
				strlist.add("interface " + className + "<<interface>> {");
			} else {
				strlist.add("class " + className + " {");
			}
			
			for (String attr: attributeMap.get(className)){
				strlist.add(attr);
			}
			
			for(String constructMethod: constructsMap.get(className)){
				strlist.add(constructMethod.replace('[', '(').replace(']', ')') );
				
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
					strlist.add(method.getKey() + parameters.toString().replace('[', '(').replace(']', ')') + ":" +returnType);
				}	
			}
			

			strlist.add("}");
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
//			System.out.println(used);
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
//						strlist.add(className + "-(0-" + use);
						if(lollipopInterfaceAndUsers.containsKey(use)) {
							lollipopInterfaceAndUsers.get(use).add(className);
						} else {
							lollipopInterfaceAndUsers.put(use, new ArrayList<String>());
							lollipopInterfaceAndUsers.get(use).add(className);
						}
						
//						if(strlist.contains(className + "..|>" + use)){
//							strlist.remove(className + "..|>" + use);
//						}
						
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
				
				
				System.out.println(str);
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
			for (int i = 0; i < strlist.size();i++) {
				if (strlist.get(i).indexOf("interface " + lollipopInterface) != -1) {

//					strsToRemove.add(strlist.get(i));
//					strlist.remove(i);
					System.out.println(strlist.get(i));
					while (strlist.get(++i).indexOf('}') == -1) {
//						System.out.println(strlist.get(i));
						strsToRemove.add(strlist.get(i));
//						strlist.remove(i);
					}
					strsToRemove.add(strlist.get(i));
					System.out.println(strlist.get(i));
//					strlist.remove(i);
				}

			}
		}
		
		for (String strToRemove: strsToRemove) {
			strlist.remove(strToRemove);
		}
		for (String strToAdd: strsToAdd) {
			System.out.println(strToAdd);
			strlist.add(strToAdd);
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
				if (multiplicityleft.equals("\"1\"") && multiplicityRight.equals("\"1\"")){
					multiplicityleft = "";
					multiplicityRight = "";
				}
				
				String mulitLeftStr = className +multiplicityleft + "--"+ multiplicityRight + associate;
				String mulitRightStr = associate + multiplicityRight+"--"+ multiplicityleft + className;
				
				if (!strlist.contains(mulitLeftStr) && !strlist.contains(mulitRightStr) 
						&& classNames.contains(associate)){
					strlist.add(mulitLeftStr);
				}
			}
		}
		
//		List<String> stringsToRemove = new ArrayList<String>();
//		List<String> stringsToAdd = new ArrayList<String>();
//		for (String str: strlist){
//			for (String intrf: ballSocketInterfaceNames){
//				if (str.endsWith(intrf)){
//					
//					if (str.endsWith("..>"+intrf)) {
//						System.out.println(str);
//						stringsToRemove.add(str);
//						String newStr1 = str.replace("..>"+intrf, "-(0-" +intrf);
//						stringsToAdd.add(newStr1);
//						continue;
//						
//					}
//					
//					
//					if (str.endsWith("..|>"+intrf)) {
//						stringsToRemove.add(str);
//						String newStr2 = str.replace("..|>"+intrf, "-()" +intrf);
//						stringsToAdd.add(newStr2);
//						continue;
//					}
//					
//					if (str.endsWith("--|>"+intrf)) {
//						System.out.println(str);
//						stringsToRemove.add(str);
//						String newStr3 = str.replace("--|>"+intrf, "--()" +intrf);
//						stringsToAdd.add(newStr3);
//						continue;
//					}
//				}
//			}
//			
//		}
//		for (String str : stringsToAdd){
//			strlist.add(str);
//		}
//		for (String str : stringsToRemove) {
//			strlist.remove(str);
//		}
		
		
		plantGrammer.addAll(strlist);
		plantGrammer.add("@enduml");
		System.out.println(plantGrammer);
		
		File file = new File(path + "-output.png");
		OutputStream png = null;
		try {
			png = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		SourceStringReader reader = new SourceStringReader(String.join("\n", plantGrammer));
		// Write the first image to "png"
		String desc = null;
		try {
			desc = reader.generateImage(png);
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		
	}
	
	
	
	public static void main(String[] args) {
		File folder = null;
		String path = "";
		
		if (args.length > 1) {
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
			
			path = args[0];
		
		}
		
		if(path != "") {
			UmlParser umlparser = new UmlParser();
			try {
				umlparser.walkDir(path);
				umlparser.generateUML(path);
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
