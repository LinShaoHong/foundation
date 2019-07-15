package com.github.sun.foundation.mybatis.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

public abstract class PersistenceConfiguration {
  private static final String CONFIG_LOCATION = "classpath:mybatis-default-config.xml";
  protected static final String DATASOURCE_NAME = "DataSource";
  protected static final String TRANSACTION_MANAGER_NAME = "TransactionManager";
  protected static final String SQL_SESSION_FACTORY_NAME = "SqlSessionFactoryBean";
  protected static final String SCANNER_NAME = "Scanner";

  protected abstract String basePackage();

  protected abstract String datasourcePrefix();

  protected SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource) throws Exception {
    SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource);
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    factoryBean.setConfigLocation(resolver.getResource(CONFIG_LOCATION));
    factoryBean.setMapperLocations(resolver.getResources(getMapperLocations(basePackage())));

    return factoryBean;
  }

  protected DataSource dataSource(Environment env) throws SQLException {
    DruidSettings setting = loadSetting(env);
    DruidDataSource dataSource = new DruidDataSource();
    dataSource.setUrl(setting.url);
    dataSource.setUsername(setting.username);
    dataSource.setPassword(setting.password);
    dataSource.setFilters(setting.filters);
    dataSource.setMaxActive(setting.maxActive);
    dataSource.setInitialSize(setting.initialSize);
    dataSource.setMinIdle(setting.minIdle);
    dataSource.setMaxWait(setting.maxWait);
    dataSource.setTimeBetweenEvictionRunsMillis(setting.timeBetweenEvictionRunsMillis);
    dataSource.setMinEvictableIdleTimeMillis(setting.minEvictableIdleTimeMillis);
    dataSource.setValidationQuery(setting.validationQuery);
    dataSource.setTestWhileIdle(setting.testWhileIdle);
    dataSource.setTestOnReturn(setting.testOnReturn);
    dataSource.setTestOnBorrow(setting.testOnBorrow);
    if (dataSource.getUrl().contains(":mysql:")) {
      dataSource.setPoolPreparedStatements(false);
    } else {
      dataSource.setPoolPreparedStatements(true);
      dataSource.setMaxPoolPreparedStatementPerConnectionSize(100);
    }
    if (setting.helperThreadSize > 0) {
      ScheduledExecutorService scheduledExecutorService = Executors
        .newScheduledThreadPool(setting.helperThreadSize);
      dataSource.setCreateScheduler(scheduledExecutorService);
      dataSource.setDestroyScheduler(scheduledExecutorService);
    }
    dataSource.init();
    return dataSource;
  }

  protected DataSourceTransactionManager transactionManager(DataSource dataSource) {
    DataSourceTransactionManager txManager = new DataSourceTransactionManager();
    txManager.setDataSource(dataSource);
    return txManager;
  }

  protected MapperScannerConfigurer scannerConfigurer(String sqlSessionFactoryName) {
    MapperScannerConfigurer scannerConfigurer = new MapperScannerConfigurer();
    scannerConfigurer.setBasePackage(basePackage());
    scannerConfigurer.setSqlSessionFactoryBeanName(sqlSessionFactoryName);
    return scannerConfigurer;
  }

  private String getMapperLocations(String basePackage) {
    return "classpath:" + basePackage.replaceAll("\\.", "/") + "/*.xml";
  }

  private DruidSettings loadSetting(Environment env) {
    String prefix = datasourcePrefix();
    Function<String, String> func = field -> prefix + "." + field;
    return new DruidSettings(
      env.getProperty(func.apply("url")),
      env.getProperty(func.apply("username")),
      env.getProperty(func.apply("password")),
      env.getProperty(func.apply("filters"), "wall,slf4j"),
      env.getProperty(func.apply("maxActive"), Integer.class, 100),
      env.getProperty(func.apply("initialSize"), Integer.class, 0),
      env.getProperty(func.apply("minIdle"), Integer.class, 0),
      env.getProperty(func.apply("maxWait"), Integer.class, 60000),
      env.getProperty(func.apply("timeBetweenEvictionRunsMillis"), Integer.class, 3000),
      env.getProperty(func.apply("minEvictableIdleTimeMillis"), Integer.class, 300000),
      env.getProperty(func.apply("validationQuery"), "SELECT 'x'"),
      env.getProperty(func.apply("testWhileIdle"), Boolean.class, true),
      env.getProperty(func.apply("testOnReturn"), Boolean.class, false),
      env.getProperty(func.apply("testOnBorrow"), Boolean.class, false),
      env.getProperty(func.apply("helperThreadSize"), Integer.class, 0)
    );
  }
}
