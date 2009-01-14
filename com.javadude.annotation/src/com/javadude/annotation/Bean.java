/*******************************************************************************
 * Copyright (c) 2008 Scott Stanchfield.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Scott Stanchfield - initial API and implementation
 *******************************************************************************/
package com.javadude.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>An annotation that requests to generate Bean code for the
 * 		annotated class.</p>
 * <p>This annotation marks a class as a JavaBean and allows generation
 * 		of code based on the specified attributes.
 *
 * <p>If you annotate a class with @Bean, the annotation processor
 * 		will generate a superclass containing the requested generated
 * 		code. The name of the generated class is the same as the
 * 		annotated class with a suffix of "Gen".
 * To use this annotation, you <b>must</b> define your class to extend
 * the generated superclass. For example:</p>
 * <pre>@Bean(...)
 * public class Foo extends FooGen { ...}</pre>
 *
 * <p>If you need to extend another class, you can ask the Bean
 * 		annotation processor to add an extends clause to the generated
 * 		class using the "superclass" attribute. For example, if you
 * 		wanted class Foo to extend class Fee, you would write:</p>
 * <pre>@Bean(superclass="Fee", ...)
 * public class Foo extends FooGen { ...}</pre>
 * <p>The generated superclass would look like</p>
 * <pre>public class FooGen extends Fee {...}</pre>
 * <p><i>If the required superclass is not in the same package as the
 * annotated class, you need to fully qualify it:</i></p>
 * <pre>@Bean(superclass="x.y.Fee",...)</pre>
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Bean {
	/**
	 * <p>Defines the class you would like to extend. The generated
	 * 		superclass will be inserted between the class you are
	 *		annotating and this class.</p>
	 * <p>You must specify the class constant for the class
	 * 		(eg: Foo.class)</p>
	 * <p>Only one of superclass or superclassString may be
	 * 		specified</p>
	 * @see #superclassString()
	 */
    Class<?> superclass() default Void.class;

    /**
     * <p>Defines the class you would like to extend.  The generated
	 *		superclass will be inserted between the class you are
	 *		annotating and this class.</p>
     * <p>If this superclass is in the same package as the annotated
     *		class, you only need to specify its name. If the
     *		superclass is in a different package, you must fully
     *		qualify it.</p>
     * <p>Only one of superclass or superclassString may be
     * 		specified</p>
     * @see #superclass()
     */
    String superclassString() default "";

    /**
     * <p>Specifies the constructor arguments in the generated
     * 		superclass.</p>
     * <p>This is only useful/necessary if you specified superclass or
     *   superclassString (the "specified superclass") and there
     *   is not a no-arg constructor in that specified superclass.
     *   These parameters can be passed to the specified superclass
     *   using the superCall attribute.</p>
     * <p>For example:</p>
     * <pre>@Bean(constructorArgs="String name, int age", ...)
     * public class Foo {...}
     * </pre>
     * <p>will generate a constructor in the generated superclass:</p>
     * <pre>public Foo (String name, int age) {...}
     * </pre>
     * @see #superclass()
     * @see #superConstructorSuperCall()
     */
    String superConstructorArgs() default "";

    /**
     * <p>Arguments to the super(...) call that will be generated
     * 		in the superclass' constructor. Used with
     * 		constructorArgs.</p>
     * <p>If you define a superclass or superclassString, and that
     * 		superclass does not have a no-arg constructor, you must
     * 		specify superCall.</p>
     * <p>For example:</p>
     * <pre>@Bean(
     *     constructorArgs="String name, int age",
     *     superCall="name, age",
     *     ...)
     * public class Foo {...}
     * </pre>
     * <p>will generate a constructor in the generated superclass:</p>
     * <pre>public Foo (String name, int age) {
     *     super(name, age);
     * }
     * </pre>
     * @see #superclass()
     * @see #superConstructorArgs()
     */
    String superConstructorSuperCall() default "";

    /**
     * <p>If true, the generated superclass will implement
     * 		java.lang.Cloneable and	appropriately override the
     * 		clone() method.</p>
     */
    boolean cloneable() default false;

    /**
     * <p>If true, the generated superclass will define an inner
     * 		interface "PropertyNames" containing all the property
     * 		names in the class. Use of this interface will reduce
     * 		typos when setting up PropertyChangeListeners.</p>
     * <p>For example, to set up a PropertyChangeListener for property
     * 		"name", you would call</p>
     * <pre>Bean bean = ...;
     * bean.addPropertyChangeListener(Bean.PropertyNames.name, ...);
     * </pre>
     * @see #extendPropertyNameConstants()
     */
    boolean definePropertyNameConstants() default false;

    /**
     * <p>If true, the property name interface will extend the
     * 		superclass' PropertyNames interface.</p>
     * @see #definePropertyNameConstants()
     */
    boolean extendPropertyNameConstants() default false;

    /**
     * <p>If non-zero, the generated code will use this many spaces
     * 		on each indented line instead of a tab.</p>
     */
    int spacesForLeadingTabs() default 0;

    /**
     * <p>If true, the superclass will add {@link Override} to the
     * 		generated paramString method and include the contents of
     * 		its superclass' paramString() method.</p>
     * @deprecated This attribute no longer does anything. The
     * 		generated superclass now has a @SuppressWarnings("all").
     * 		You should remove this attribute.
     */
    @Deprecated
    boolean overrideParamString() default false;

    /**
     * <p>If true, the generated superclass will include a simple
     * 		equals() and hashCode() method.</p>
     * @see #equalsShouldCheckSuperEquals()
     */
    boolean defineSimpleEqualsAndHashCode() default false;

    /**
     * <p>If true, the generated superclass' equals() method will
     * 		first check super.equals(), and return false if its
     * 		result is false.</p>
     * @see #defineSimpleEqualsAndHashCode()
     */
    boolean equalsShouldCheckSuperEquals() default false;

    /**
     * <p>If true, generate a createPropertyMap() method in the
     * 		superclass. This method will generate a
     * 		<code>Map<String, Object><code> that contains property
     * 		name/values for properties defined via an {@link Property}
     * 		annotation inside this bean.</p>
     * @see #createPropertyMapCallsSuper()
     */
    boolean createPropertyMap() default false;

    /**
     * <p>If true, the generated createPropertyMap() will call
     * 		super.createPropertyMap().</p>
     * @see #createPropertyMap()
     */
    boolean createPropertyMapCallsSuper() default false;

    /**
     * <p>If true, and createPropertyMap() is true, the generated
     * 		createPropertyMap() method will be annotated
     * 		@Override.</p>
     * @deprecated This attribute no longer does anything. The
     * 		generated superclass now has a @SuppressWarnings("all").
     * 		You should remove this attribute.
     * @see #createPropertyMap()
     */
    @Deprecated
    boolean createPropertyMapNeedsOverride() default false;

    /**
     * <p>A list of {@link Property} annotations that define
     * 		properties to be generated in the generated superclass.</p>
     * @see Property
     */
    Property[] properties() default { };

    /**
     * <p>A list of {@link Observer} annotations that specify
     * 		observer pattern creation in the generated superclass.</p>
     * @see Observer
     */
    Observer[] observers() default { };

    /**
     * <p>A list of {@link Delegate} annotations that specify
     * 		creation of delegate methods in the generated
     * 		superclass.</p>
     * @see Delegate
     */
    Delegate[] delegates() default { };

    /**
     * A list of {@link NullObject} annotations that specify creation
     * 		of null stub methods in the generated superclass.</p>
     * @see NullObject
     */
    NullObject[] nullObjectImplementations() default { };

    /**
     * <p>Specifies the default access level of the setter
     * 		methods generated for any defined properties</p>
     * @see Access
     * @see Property
     */
    Access reader() default Access.PUBLIC;

    /**
     * <p>Specifies the default access level of the getter methods
     * 		generated for any defined properties</p>
     * @see Access
     * @see Property
     */
    Access writer() default Access.PUBLIC;
}
