package com.github.sun.foundation.spring.rest.config;

import com.github.sun.foundation.mybatis.config.PersistenceConfiguration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class RestServerPersisConfiguration extends PersistenceConfiguration {
  private static final String ID = "restServer";

  @Override
  protected String id() {
    return ID;
  }

  @Override
  protected String basePackage() {
    return "com.github.sun.foundation.spring.rest.mapper";
  }

  @Primary
  @Bean(name = ID + SQL_SESSION_FACTORY_NAME)
  public SqlSessionFactoryBean sqlSessionFactoryBean(Environment env) throws Exception {
    return super.sqlSessionFactoryBean(dataSource(env));
  }

  @Primary
  @Bean(name = ID + DATASOURCE_NAME)
  public DataSource dataSource(Environment env) {
    return super.dataSource(env);
  }

  @Primary
  @Bean(name = ID + TRANSACTION_MANAGER_NAME)
  public DataSourceTransactionManager transactionManager(Environment env) {
    return super.transactionManager(dataSource(env));
  }

  @Primary
  @Bean(name = ID + SCANNER_NAME)
  protected MapperScannerConfigurer scannerConfigurer() {
    return super.scannerConfigurer(ID + SQL_SESSION_FACTORY_NAME);
  }
}
