/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.test.util.impl;

import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.ogm.util.impl.ConfigurationPropertyReader;
import org.hibernate.ogm.util.impl.ConfigurationPropertyReader.Instantiator;
import org.hibernate.ogm.util.impl.ConfigurationPropertyReader.ShortNameResolver;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit test for {@link ConfigurationPropertyReader}.
 *
 * @author Gunnar Morling
 */
public class ConfigurationPropertyReaderTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void shouldRetrievePropertyWithInstanceValue() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", new MyServiceImpl() );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class ).getValue();

		assertThat( value.getClass() ).isEqualTo( MyServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyWithClassValue() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", MyServiceImpl.class );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class ).getValue();

		assertThat( value.getClass() ).isEqualTo( MyServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyWithStringValue() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", MyServiceImpl.class.getName() );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class ).getValue();

		assertThat( value.getClass() ).isEqualTo( MyServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyWithDefaultImplementation() {
		Map<String, Object> properties = new HashMap<String, Object>();

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class )
				.withDefaultImplementation( MyOtherServiceImpl.class )
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyOtherServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyWithShortName() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", "other" );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class )
				.withShortNameResolver( new MyShortNameResolver() )
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyOtherServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyWithDefaultImplementationName() {
		Map<String, Object> properties = new HashMap<String, Object>();

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class )
				.withDefaultImplementation( MyServiceImpl.class.getName() )
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyServiceImpl.class );
	}

	@Test
	public void shouldRetrievePropertyUsingCustomInstantiator() {
		Map<String, Object> properties = new HashMap<String, Object>();

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );
		MyService value = reader.property( "service", MyService.class )
				.withDefaultImplementation( MyYetAnotherServiceImpl.class )
				.withInstantiator( new MyInstantiator() )
				.getValue();

		assertThat( value.getClass() ).isEqualTo( MyYetAnotherServiceImpl.class );
	}

	@Test
	public void shouldRaiseExceptionDueToWrongInstanceType() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000046" );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", 42 );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );

		reader.property( "service", MyService.class ).getValue();
	}

	@Test
	public void shouldRaiseExceptionDueToWrongClassType() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000045" );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", Integer.class );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );

		reader.property( "service", MyService.class ).getValue();
	}

	@Test
	public void shouldRaiseExceptionDueToWrongClassTypeGivenAsString() {
		thrown.expect( HibernateException.class );
		thrown.expectMessage( "OGM000045" );

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put( "service", Integer.class.getName() );

		ConfigurationPropertyReader reader = new ConfigurationPropertyReader( properties, new ClassLoaderServiceImpl() );

		reader.property( "service", MyService.class ).getValue();
	}

	private interface MyService {
	}

	public static class MyServiceImpl implements MyService {
	}

	public static class MyOtherServiceImpl implements MyService {
	}

	public static class MyYetAnotherServiceImpl implements MyService {

		public MyYetAnotherServiceImpl(String name) {
		}
	}

	private static class MyShortNameResolver implements ShortNameResolver {

		@Override
		public boolean isShortName(String name) {
			return "other".equals( name );
		}

		@Override
		public String resolve(String shortName) {
			return MyOtherServiceImpl.class.getName();
		}
	}

	private static class MyInstantiator implements Instantiator<MyService> {

		@Override
		public MyService newInstance(Class<? extends MyService> clazz) {
			return new MyYetAnotherServiceImpl( "foo" );
		}
	}
}
