package umlparser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;


public class FieldParser {
	public static final ArrayList<String> reservedTypes = new ArrayList<String>(Arrays.asList("byte","short","int","long","float","double","boolean","char","Integer","String", "Character"));
	public static final ArrayList<String> multiples = new ArrayList<String>(Arrays.asList("List","Set","Collection","ArrayList","LinkedList","HashSet"));
	
	private List<String> attributes; //primitive attributes like int, String
	private List<String> associates; //attributes of objects of other classes in this program
	private Map<String, String> multiplicityMap;
	
	public FieldParser(){
		attributes = new ArrayList<String>();
		associates = new ArrayList<String>();	
		multiplicityMap = new HashMap<String, String>();

	}
	
	public void visit(CompilationUnit cu){
		List<BodyDeclaration> bds = cu.getTypes().get(0).getMembers();
		for (BodyDeclaration bd : bds) {

			String[] str = bd.toStringWithoutComments().split(" ");
			String modifier ="";
			if (str.length == 2){
				//for default or package
				modifier = "~";
			} else {
				switch(str[0]){
				case "private": modifier = "-";
				break;
				case "protected": modifier = "#";
				break;
				case "public": modifier = "+";
				break;
				case "package":modifier ="~";
				}
			}
			
			String attributeString ="";
			String associateString ="";
			String typeString = str[str.length - 2];
			String fieldString = str[str.length - 1];
			
			
			String generalizedtype = " ";
			int leftBracketIndex = typeString.indexOf('<');
			int rightBracketIndex = typeString.indexOf('>');
			if (leftBracketIndex != -1 && rightBracketIndex != -1){
				generalizedtype = typeString.substring(leftBracketIndex+1, rightBracketIndex);
				
			}
			
			for (String reservedType : reservedTypes) {
				if( typeString.startsWith(reservedType) || reservedType.equals(generalizedtype)) {
					attributeString = modifier + fieldString.substring(0,fieldString.length() - 1) + ":" +  reservedType;
				}
				
			}
			
//			System.out.println(typeString);
			int indexOfLeftSquare = typeString.indexOf('[');
			int indexOfRightSquare = typeString.indexOf(']');
			if (!attributeString.equals("") && attributeString.indexOf('#') == -1 && attributeString.indexOf('~') == -1) {
//				System.out.println(attributeString);
				for (String multiStr: multiples) {
					if(typeString.indexOf(multiStr) != -1){
						attributeString += "(*)";
						break;
					}
				}
				
				if( indexOfLeftSquare != -1 &&  indexOfRightSquare != -1 ){
					if (indexOfRightSquare - indexOfLeftSquare == 1) {
						attributeString += "(*)";
					} else {
						attributeString += "[" +typeString.substring(indexOfLeftSquare + 1, indexOfRightSquare) + "]";
					}
					
				}
//				System.out.println(attributeString);
				attributes.add(attributeString);
				
			} else {
//				System.out.println(typeString);
				if (!generalizedtype.equals("")) {
					associateString = generalizedtype;
					for (String multiStr : multiples) {
						if (typeString.indexOf(multiStr) != -1) {
							 multiplicityMap.put(associateString,"*");
						}
					}
//					System.out.println(typeString);
					
				} 
				if (typeString.indexOf("<") == -1){
					associateString = typeString;//.substring(0,indexOfLeftSquare);
//					System.out.println(associateString);

				}
				
				if (associateString != " "){
//					System.out.println(associateString);
					associates.add(associateString);
				}
			}


		}
	}
	
	
	
	public List<String> getAttributes(){
		return this.attributes;
	}
	
	public List<String> getAssociates(){	
		return this.associates;
	}
	
	public Map<String, String> getMultiplicityMap(){
		return this.multiplicityMap;
	}

}
