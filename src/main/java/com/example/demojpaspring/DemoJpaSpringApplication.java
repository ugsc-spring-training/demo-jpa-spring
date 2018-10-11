package com.example.demojpaspring;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import javax.sql.DataSource;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
@ComponentScan
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.example.demojpaspring")
public class DemoJpaSpringApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoJpaSpringApplication.class, args);
	}

	@Bean
	public DataSource dataSource() {
		return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
	}

	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
		HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
		adapter.setGenerateDdl(true);
		adapter.setShowSql(true);

		Properties properties = new Properties();
		properties.setProperty("hibernate.format_sql", "true");

		LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
		emf.setDataSource(dataSource);
		emf.setPackagesToScan("com.example.demojpaspring");
		emf.setJpaVendorAdapter(adapter);
		emf.setJpaProperties(properties);

		return emf;
	}

	@Bean
	public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
		return new JpaTransactionManager(emf);
	}
}

@Component
class MyRunner implements CommandLineRunner {

	@PersistenceContext
	private EntityManager em;

	private final MyRepository personRepository;

	MyRunner(MyRepository personRepository) {
		this.personRepository = personRepository;
	}

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		Set<Address> addressSet = Stream.of("Nitra,Sala,Komarom".split(","))
				.map(t -> new Address(null, t))
				.collect(Collectors.toSet());

		addressSet.forEach(em::persist);
		em.persist(new Person(null, "Miso", addressSet));

		System.out.println(em.createQuery("select p from Person p").getResultList());

		System.out.println(personRepository.findAll());
		System.out.println("findAllByName:");
		System.out.println(personRepository.findAllByName("Miso"));
		System.out.println("findAllMisos");
		System.out.println(personRepository.findAllMisos());
		System.out.println("findPersonMiso");
		System.out.println(personRepository.findPersonMiso());
	}
}

interface PersonRepository extends JpaRepository<Person, Long> {
	List<Person> findAllByName(String name);

	@Query(value = "select p from Person p where p.name='Miso'")
	public Person findAllMisos();
}

interface MyRepository extends PersonRepository {
	public Person findPersonMiso();
}

class MyRepositoryImpl {
	@PersistenceContext
	EntityManager em;

	public Person findPersonMiso() {
		return em.createQuery("select p from Person p where p.name='Miso'", Person.class)
				.getSingleResult();
	}
}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
class Person {
	@Id @GeneratedValue
	private Long id;
	private String name;
	@OneToMany(fetch = FetchType.EAGER)
	private Set<Address> adrressSet;
}

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
class Address {
	@Id @GeneratedValue
	private Long id;
	private String city;
}
