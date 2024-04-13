/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.openAiServlet;


import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.javaparser.ast.Modifier;


/**
 * A simple DS component which is executed every 10 seconds
 *
 * @see <a href="https://sling.apache.org/documentation/bundles/scheduler-service-commons-scheduler.html">Scheduler Service</a>
 */
@Component( property = {
    "scheduler.period:Long=10"
})
public class SimpleDSComponent implements Runnable {

    public static void main(String[] args) {
        // Create a new CompilationUnit
        CompilationUnit cu = new CompilationUnit();

        // Add package declaration
        cu.setPackageDeclaration("com.example");

        // Add imports if needed
        cu.addImport("java.util.List");

        // Create a class
        ClassOrInterfaceDeclaration classDeclaration = cu.addClass("MyClass");
        classDeclaration.setModifiers(Modifier.publicModifier().getKeyword());

        // Add fields
        FieldDeclaration fieldDeclaration = classDeclaration.addField("int", "myField");
        fieldDeclaration.getVariables().get(0).setInitializer("42"); // Set initial value

        // Add methods
        Type returnType = PrimitiveType.intType(); // Return type
        MethodDeclaration methodDeclaration = classDeclaration.addMethod("myMethod", Modifier.publicModifier().getKeyword());
        methodDeclaration.setType(ret   urnType);
        methodDeclaration.getBody().ifPresent(body ->
                body.addStatement("return this.myField * 2;"));

        // Generate Java code
        String generatedCode = cu.toString();
        System.out.println(generatedCode);
    }
    
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private BundleContext bundleContext;
    
    public void run() {
    }
    
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
    }
    
    protected void deactivate(ComponentContext ctx) {
        this.bundleContext = null;
    }

}

