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
package org.hibernate.ogm.test.couchdb.associations;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.ogm.datastore.couchdb.CouchDB;
import org.hibernate.ogm.options.couchdb.AssociationStorageType;
import org.hibernate.ogm.test.associations.collection.unidirectional.Cloud;
import org.hibernate.ogm.test.associations.collection.unidirectional.SnowFlake;
import org.junit.After;
import org.junit.Test;

/**
 * Test for configuring the different association storage modes via the option API.
 *
 * @author Gunnar Morling
 */
public class AssociationStorageConfiguredViaApiTest extends AssociationStorageTestBase {

	private Cloud cloud;

	@Test
	public void associationStorageSetToAssociationDocumentOnGlobalLevel() throws Exception {
		configuration.configureOptionsFor( CouchDB.class )
			.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 0 );
	}

	@Test
	public void associationStorageSetToInEntityOnGlobalLevel() throws Exception {
		configuration.configureOptionsFor( CouchDB.class )
			.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 0 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	public void associationStorageSetToAssociationDocumentOnEntityLevel() throws Exception {
		configuration.configureOptionsFor( CouchDB.class )
			.entity( Cloud.class )
				.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 0 );
	}

	@Test
	public void associationStorageSetToInEntityOnEntityLevel() throws Exception {
		configuration.configureOptionsFor( CouchDB.class )
			.entity( Cloud.class )
				.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory();
		createCloudWithTwoProducedSnowflakes();

		assertThat( associationDocumentCount() ).isEqualTo( 0 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	public void associationStorageSetOnPropertyLevel() throws Exception {
		configuration.configureOptionsFor( CouchDB.class )
			.entity( Cloud.class )
				.property( "producedSnowFlakes", ElementType.METHOD )
					.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT )
				.property( "backupSnowFlakes", ElementType.METHOD )
					.associationStorage( AssociationStorageType.IN_ENTITY );

		setupSessionFactory();
		createCloudWithTwoProducedAndOneBackupSnowflake();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	@Test
	public void associationStorageSetOnPropertyLevelTakesPrecedenceOverEntityLevel() throws Exception {
		configuration.configureOptionsFor( CouchDB.class )
		.entity( Cloud.class )
			.associationStorage( AssociationStorageType.IN_ENTITY )
			.property( "backupSnowFlakes", ElementType.METHOD )
				.associationStorage( AssociationStorageType.ASSOCIATION_DOCUMENT );

		setupSessionFactory();
		createCloudWithTwoProducedAndOneBackupSnowflake();

		assertThat( associationDocumentCount() ).isEqualTo( 1 );
		assertThat( inEntityAssociationCount() ).isEqualTo( 1 );
	}

	private void createCloudWithTwoProducedSnowflakes() {
		cloud = newCloud()
				.withLength( 23 )
				.withProducedSnowflakes( "Snowflake1", "Snowflake2" )
				.createAndSave();
	}

	private void createCloudWithTwoProducedAndOneBackupSnowflake() {
		cloud = newCloud()
				.withLength( 23 )
				.withProducedSnowflakes( "Snowflake1", "Snowflake2" )
				.withBackupSnowflakes( "Snowflake3" )
				.createAndSave();
	}

	private CloudBuilder newCloud() {
		return new CloudBuilder();
	}

	private class CloudBuilder {

		private int length;
		private final List<String> producedSnowflakes = new ArrayList<String>();
		private final List<String> backupSnowflakes = new ArrayList<String>();

		public CloudBuilder withLength(int length) {
			this.length = length;
			return this;
		}

		public CloudBuilder withProducedSnowflakes(String... descriptions) {
			this.producedSnowflakes.addAll( Arrays.asList( descriptions ) );
			return this;
		}

		public CloudBuilder withBackupSnowflakes(String... descriptions) {
			this.backupSnowflakes.addAll( Arrays.asList( descriptions ) );
			return this;
		}

		public Cloud createAndSave() {
			Session session = sessions.openSession();
			Transaction transaction = session.beginTransaction();

			Cloud cloud = new Cloud();
			cloud.setLength( length );

			for ( String description : producedSnowflakes ) {
				SnowFlake sf = new SnowFlake();
				sf.setDescription( description );
				session.save( sf );
				cloud.getProducedSnowFlakes().add( sf );
			}

			for ( String description : backupSnowflakes ) {
				SnowFlake sf = new SnowFlake();
				sf.setDescription( description );
				session.save( sf );
				cloud.getBackupSnowFlakes().add( sf );
			}

			session.persist( cloud );

			transaction.commit();
			session.close();

			return cloud;
		}
	}

	@After
	public void removeCloudAndSnowflakes() {
		Session session = sessions.openSession();
		Transaction transaction = session.beginTransaction();

		if ( cloud != null ) {
			Cloud cloudToDelete = (Cloud) session.get( Cloud.class, cloud.getId() );
			for ( SnowFlake current : cloudToDelete.getProducedSnowFlakes() ) {
				session.delete( current );
			}
			for ( SnowFlake current : cloudToDelete.getBackupSnowFlakes() ) {
				session.delete( current );
			}
			session.delete( cloudToDelete );
		}

		transaction.commit();
		session.close();

		assertThat( testHelper.getNumberOfEntities( sessions ) ).isEqualTo( 0 );
		assertThat( testHelper.getNumberOfAssociations( sessions ) ).isEqualTo( 0 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Cloud.class,
				SnowFlake.class
		};
	}
}
