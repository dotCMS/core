package com.dotmarketing.compilers;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.IErrorHandlingPolicy;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Logger;

public class DotJdtCompiler {
	
       /** 
        * Compiles the given java file and generates the class file on the given output dir
        * @return A list of compilation problems if that list contains compilation errors it might be that the class
        * couldn't get compiled
        */
       public static DotCompilationProblems compileClass(String mySourceFile, String myClassName, String myOutputDir)
           throws FileNotFoundException, DotRuntimeException {
   
           final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
           final String sourceFile = mySourceFile;
           final String targetClassName = myClassName;
           final ArrayList<IProblem> problemList = new ArrayList<IProblem>();
           final String outputDir = myOutputDir;
           String[] fileNames = new String[] {sourceFile};
           String[] classNames = new String[] {targetClassName};
           
           class CompilationUnit implements ICompilationUnit {
   
               String className;
               String sourceFile;
   
               CompilationUnit(String sourceFile, String className) {
                   this.className = className;
                   this.sourceFile = sourceFile;
               }
   
               public char[] getFileName() {
                   return sourceFile.toCharArray();
               }
               
               public char[] getContents() {
                   char[] result = null;
                   FileInputStream is = null;
                   try {
                       is = new FileInputStream(sourceFile);
                       Reader reader = 
                           new BufferedReader(new InputStreamReader(is, "UTF-8"));
                       if (reader != null) {
                           char[] chars = new char[8192];
                           StringBuffer buf = new StringBuffer();
                           int count;
                           while ((count = reader.read(chars, 0, 
                                                       chars.length)) > 0) {
                               buf.append(chars, 0, count);
                           }
                           result = new char[buf.length()];
                           buf.getChars(0, result.length, result, 0);
                       }
                   } catch (IOException e) {
                       Logger.error(this, "Compilation error", e);
                   } finally {
                       if (is != null) {
                           try {
                               is.close();
                           } catch (IOException exc) {
                               // Ignore
                           }
                       }
                   }
                   return result;
               }
               
               public char[] getMainTypeName() {
                   int dot = className.lastIndexOf('.');
                   if (dot > 0) {
                       return className.substring(dot + 1).toCharArray();
                   }
                   return className.toCharArray();
               }
               
               public char[][] getPackageName() {
                   StringTokenizer izer = 
                       new StringTokenizer(className, ".");
                   char[][] result = new char[izer.countTokens()-1][];
                   for (int i = 0; i < result.length; i++) {
                       String tok = izer.nextToken();
                       result[i] = tok.toCharArray();
                   }
                   return result;
               }
           }
   
           final INameEnvironment env = new INameEnvironment() {
   
                   public NameEnvironmentAnswer 
                       findType(char[][] compoundTypeName) {
                       String result = "";
                       String sep = "";
                       for (int i = 0; i < compoundTypeName.length; i++) {
                           result += sep;
                           result += new String(compoundTypeName[i]);
                           sep = ".";
                       }
                       return findType(result);
                   }
   
                   public NameEnvironmentAnswer 
                       findType(char[] typeName, 
                                char[][] packageName) {
                           String result = "";
                           String sep = "";
                           for (int i = 0; i < packageName.length; i++) {
                               result += sep;
                               result += new String(packageName[i]);
                               sep = ".";
                           }
                           result += sep;
                           result += new String(typeName);
                           return findType(result);
                   }
                   
                   private NameEnvironmentAnswer findType(String className) {
   
                       InputStream is = null;
                       try {
                           if (className.equals(targetClassName)) {
                               ICompilationUnit compilationUnit = 
                                   new CompilationUnit(sourceFile, className);
                               return 
                                   new NameEnvironmentAnswer(compilationUnit, null);
                           }
                           String resourceName = 
                               className.replace('.', '/') + ".class";
                           is = classLoader.getResourceAsStream(resourceName);
                           if (is != null) {
                               byte[] classBytes;
                               byte[] buf = new byte[8192];
                               ByteArrayOutputStream baos = 
                                   new ByteArrayOutputStream(buf.length);
                               int count;
                               while ((count = is.read(buf, 0, buf.length)) > 0) {
                                   baos.write(buf, 0, count);
                               }
                               baos.flush();
                               classBytes = baos.toByteArray();
                               char[] fileName = className.toCharArray();
                               ClassFileReader classFileReader = 
                                   new ClassFileReader(classBytes, fileName, 
                                                       true);
                               return 
                                   new NameEnvironmentAnswer(classFileReader, null);
                           }
                       } catch (IOException exc) {
                           Logger.error(this, "Compilation error", exc);
                       } catch (ClassFormatException exc) {
                           Logger.error(this, "Compilation error", exc);
                       } finally {
                           if (is != null) {
                               try {
                                   is.close();
                               } catch (IOException exc) {
                                   // Ignore
                               }
                           }
                       }
                       return null;
                   }
   
                   private boolean isPackage(String result) {
                       if (result.equals(targetClassName)) {
                           return false;
                       }
                       String resourceName = result.replace('.', '/') + ".class";
                       InputStream is = 
                           classLoader.getResourceAsStream(resourceName);
                       return is == null;
                   }
   
                   public boolean isPackage(char[][] parentPackageName, 
                                            char[] packageName) {
                       String result = "";
                       String sep = "";
                       if (parentPackageName != null) {
                           for (int i = 0; i < parentPackageName.length; i++) {
                               result += sep;
                               String str = new String(parentPackageName[i]);
                               result += str;
                               sep = ".";
                           }
                       }
                       String str = new String(packageName);
                       if (Character.isUpperCase(str.charAt(0))) {
                           if (!isPackage(result)) {
                               return false;
                           }
                       }
                       result += sep;
                       result += str;
                       return isPackage(result);
                   }
   
                   public void cleanup() {
                   }
   
               };
   
           final IErrorHandlingPolicy policy = 
               DefaultErrorHandlingPolicies.proceedWithAllProblems();
   
           
           final Map<String, String> settings = new HashMap<String, String>();
           settings.put(CompilerOptions.OPTION_LineNumberAttribute,
                        CompilerOptions.GENERATE);
           settings.put(CompilerOptions.OPTION_SourceFileAttribute,
                        CompilerOptions.GENERATE);
           settings.put(CompilerOptions.OPTION_ReportDeprecation,
                        CompilerOptions.IGNORE);
           settings.put(CompilerOptions.OPTION_Encoding,
                        "UTF-8");
           settings.put(CompilerOptions.OPTION_LocalVariableAttribute,
                        CompilerOptions.GENERATE);
   
           // Source JVM

           // Default to 1.5
           settings.put(CompilerOptions.OPTION_Source,
                       CompilerOptions.VERSION_1_5);
           
           // Target JVM
           // Default to 1.5
           settings.put(CompilerOptions.OPTION_TargetPlatform,
                       CompilerOptions.VERSION_1_5);
           settings.put(CompilerOptions.OPTION_Compliance,
                       CompilerOptions.VERSION_1_5);
   
           final CompilerOptions coptions = new CompilerOptions(settings);
           
           final IProblemFactory problemFactory = 
               new DefaultProblemFactory(Locale.getDefault());
           
           final ICompilerRequestor requestor = new ICompilerRequestor() {
                   public void acceptResult(CompilationResult cresult) {
                       try {
                           if (cresult.hasProblems()) {
                               IProblem[] cproblems = cresult.getProblems();
                               for (int i = 0; i < cproblems.length; i++) {
                                   IProblem problem = cproblems[i];
                                   if (problem.isError()) {
                                       problemList.add(problem);
                                   }
                               }
                           }
                           if (problemList.isEmpty()) {
                               ClassFile[] classFiles = cresult.getClassFiles();
                               for (int i = 0; i < classFiles.length; i++) {
                                   ClassFile classFile = classFiles[i];
                                   char[][] compoundName = 
                                       classFile.getCompoundName();
                                   String className = "";
                                   String sep = "";
                                   for (int j = 0; 
                                        j < compoundName.length; j++) {
                                       className += sep;
                                       className += new String(compoundName[j]);
                                       sep = ".";
                                   }
                                   byte[] bytes = classFile.getBytes();
                                   String outFile = outputDir + "/" + 
                                       className.replace('.', '/') + ".class";
                                   
                                   File parentFolder = new File(new File(outFile).getParent());
                                   if(!parentFolder.exists())
                                	   parentFolder.mkdirs();
                                   
                                   FileOutputStream fout = 
                                       new FileOutputStream(outFile);
                                   BufferedOutputStream bos = 
                                       new BufferedOutputStream(fout);
                                   bos.write(bytes);
                                   bos.close();
                               }
                           }
                       } catch (IOException exc) {
                           Logger.error(this, "Compilation error", exc);
                       }
                   }
               };
   
           ICompilationUnit[] compilationUnits = 
               new ICompilationUnit[classNames.length];
           for (int i = 0; i < compilationUnits.length; i++) {
               String className = classNames[i];
               compilationUnits[i] = new CompilationUnit(fileNames[i], className);
           }
           Compiler compiler = new Compiler(env,
                                            policy,
                                            coptions,
                                            requestor,
                                            problemFactory);
           compiler.compile(compilationUnits);
   
           List<DotCompilationProblem> dotProblemsList = new ArrayList<DotCompilationProblem> ();
           if (!problemList.isEmpty()) {
               for(IProblem problem : problemList) {
            	  DotCompilationProblem p = new DotCompilationProblem(new String(problem.getOriginatingFileName()), 
            			  problem.getMessage(), problem.getSourceLineNumber(), problem.isError());
            	  dotProblemsList.add(p);
               }               
           }
           
           return new DotCompilationProblems(dotProblemsList);
           
       }
       
}
