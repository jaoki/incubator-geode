/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.internal.logging;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import com.gemstone.gemfire.InternalGemFireError;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;
import com.gemstone.gemfire.distributed.internal.InternalDistributedSystem;
import com.gemstone.gemfire.internal.logging.log4j.AppenderContext;
import com.gemstone.gemfire.internal.logging.log4j.ConfigLocator;
import com.gemstone.gemfire.internal.logging.log4j.Configurator;
import com.gemstone.gemfire.internal.logging.log4j.FastLogger;
import com.gemstone.gemfire.internal.logging.log4j.LogMarker;
import com.gemstone.gemfire.internal.logging.log4j.LogWriterLogger;
import com.gemstone.gemfire.internal.util.IOUtils;
import com.gemstone.org.apache.logging.log4j.core.config.xml.GemFireXmlConfiguration;
import com.gemstone.org.apache.logging.log4j.core.config.xml.GemFireXmlConfigurationFactory;
import com.gemstone.org.apache.logging.log4j.message.GemFireParameterizedMessageFactory;

/**
 * Centralizes log configuration and initialization.
 * 
 * @author bakera
 * @author David Hoots
 */
@SuppressWarnings("unused")
public class LogService extends LogManager {
  // This is highest point in the hierarchy for all GemFire logging
  public static final String ROOT_LOGGER_NAME = "";
  public static final String BASE_LOGGER_NAME = "com.gemstone";
  public static final String MAIN_LOGGER_NAME = "com.gemstone.gemfire";
  public static final String SECURITY_LOGGER_NAME = "com.gemstone.gemfire.security";
  
  public static final String GEMFIRE_VERBOSE_FILTER = "{GEMFIRE_VERBOSE}";
  
  protected static final String STDOUT = "STDOUT";

  private static final PropertyChangeListener propertyChangeListener = new PropertyChangeListenerImpl();
  
  public static final String DEFAULT_CONFIG = "/com/gemstone/gemfire/internal/logging/log4j/log4j2-default.xml";
  public static final String CLI_CONFIG = "/com/gemstone/gemfire/internal/logging/log4j/log4j2-cli.xml";

  /** Protected by static synchronization. Used for removal and adding stdout back in. */
  private static Appender stdoutAppender;
  
  /** Set with info for Banner to log anytime LogService is reinitialized */
  private static String configFileInformation;
  
  static {
    init();
  }
  private static void init() {
    setLog4jConfigFileProperty();
    LoggerContext context = ((org.apache.logging.log4j.core.Logger) LogManager.getLogger(BASE_LOGGER_NAME, GemFireParameterizedMessageFactory.INSTANCE)).getContext();
    context.removePropertyChangeListener(propertyChangeListener);
    context.addPropertyChangeListener(propertyChangeListener);
    context.reconfigure(); // propertyChangeListener invokes configureFastLoggerDelegating
    configureLoggers(false, false);
  }
  
  public static void initialize() {
    new LogService();
  }
  
  public static void reconfigure() {
    init();
  }
  
  public static void configureLoggers(final boolean hasLogFile, final boolean hasSecurityLogFile) {
    Configurator.getOrCreateLoggerConfig(BASE_LOGGER_NAME, true, false);
    Configurator.getOrCreateLoggerConfig(MAIN_LOGGER_NAME, true, hasLogFile);
    final boolean useMainLoggerForSecurity = !hasSecurityLogFile;
    Configurator.getOrCreateLoggerConfig(SECURITY_LOGGER_NAME, useMainLoggerForSecurity, hasSecurityLogFile);
  }
  
  public static AppenderContext getAppenderContext() {
    return new AppenderContext();
  }
  
  public static AppenderContext getAppenderContext(final String name) {
    return new AppenderContext(name);
  }
  
  public static boolean isUsingGemFireDefaultConfig() {
    final String configFileName = new StrSubstitutor(new Interpolator()).replace(
        PropertiesUtil.getProperties().getStringProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY));
    return configFileName == null ? false : configFileName.contains(DEFAULT_CONFIG);
  }
  
  /**
   * Check to see if the user has specified a Log4j configuration file.  If not, attempt
   * to find a GemFire Log4j configuration file in various locations.
   * 
   * @return true if log4j.configurationFile property was set; false if it was unchanged
   */
  private static final boolean setLog4jConfigFileProperty() {
    // fix bug #52175
    final URL configInClasspath = ConfigLocator.findConfigInClasspath();
    if (configInClasspath != null ) {
      // Log4J 2 will find the configuration file in classpath so do nothing
      configFileInformation = "Using log4j configuration found in classpath: '" + configInClasspath.toString() + "'";
      StatusLogger.getLogger().info(configFileInformation);
      return false;
    }
    
    // If the user set the log4j system property then there's nothing else to do.
    final String configFileName = new StrSubstitutor(new Interpolator()).replace(
        PropertiesUtil.getProperties().getStringProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY));
    if (configFileName != null) {
      final URL configUrl = LogService.class.getResource(configFileName);// log4j2-cli.xml is non-null, external is null
      if (configUrl == null) {
        //We will let log4j2 handle the null case and just log what file we are attempting to use
        configFileInformation = "Using log4j configuration file specified by " + ConfigurationFactory.CONFIGURATION_FILE_PROPERTY + ": '" + configFileName + "'";
        StatusLogger.getLogger().info(configFileInformation);
        return false;
      }
      else {
        //If the resource can be found and in cases where the resource is in gemfire jar,
        //we set the log location to the file that was found

        // must change from Java resource syntax to Java URL syntax (GEODE-232)
        // jar:file:/export/latvia1/users/klund/dev/asf-geode/gemfire-assembly/build/install/apache-geode/lib/gemfire-core-1.0.0-incubating-SNAPSHOT.jar!/com/gemstone/gemfire/internal/logging/log4j/log4j2-cli.xml
        
        String configFilePropertyValue = configUrl.toString();
        // configFileName is Java resource syntax, configFilePropertyValue is URL syntax as required by log4j2
        
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, configFilePropertyValue);
        configFileInformation = "Using log4j configuration file specified by " + ConfigurationFactory.CONFIGURATION_FILE_PROPERTY + ": '" + configFilePropertyValue + "'";
        StatusLogger.getLogger().info(configFileInformation);
        return true;
      }
    }
    
//    // set log4j.configurationFactory to be our optimized version
//    final String factory = GemFireXmlConfigurationFactory.class.getName();
//    System.setProperty(ConfigurationFactory.CONFIGURATION_FACTORY_PROPERTY, factory);
//    StatusLogger.getLogger().debug("Using log4j configuration factory '{}'", factory);
    
    // If one of the default log4j config files exists in the current directory then use it.
    File log4jConfigFile = findLog4jConfigInCurrentDir();
    if (log4jConfigFile != null) {
      String filePath = IOUtils.tryGetCanonicalPathElseGetAbsolutePath(log4jConfigFile);
      String value = new File(filePath).toURI().toString();
      String configFilePropertyValue = new File(filePath).toURI().toString();
      System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, configFilePropertyValue);
      configFileInformation = "Setting " + ConfigurationFactory.CONFIGURATION_FILE_PROPERTY + " to specify log4j configuration file in current directory: '" + configFilePropertyValue + "'";
      StatusLogger.getLogger().debug(configFileInformation);
      return true;
    }

    // Use the log4j config file found on the classpath in the gemfire jar file.
    final URL configUrl = LogService.class.getResource(DEFAULT_CONFIG);
    String configFilePropertyValue = configUrl.toString();
    System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, configFilePropertyValue);
    configFileInformation = "Setting " + ConfigurationFactory.CONFIGURATION_FILE_PROPERTY + " to specify log4j configuration file: '" + configFilePropertyValue + "'";
    StatusLogger.getLogger().info(configFileInformation);
    return true;
  }
  
  public static String getConfigInformation() {
    return configFileInformation;
  }

  /**
   * Finds a Log4j configuration file in the current directory.  The names of
   * the files to look for are the same as those that Log4j would look for on
   * the classpath.
   * 
   * @return A File for the configuration file or null if one isn't found.
   */
  public static File findLog4jConfigInCurrentDir() {    
    return ConfigLocator.findConfigInWorkingDirectory();
  }

  /**
   * Returns a Logger with the name of the calling class.
   * @return The Logger for the calling class.
   */
  public static Logger getLogger() {
    return new FastLogger(LogManager.getLogger(getClassName(2), GemFireParameterizedMessageFactory.INSTANCE));
  }
  
  public static Logger getLogger(final String name) {
    return new FastLogger(LogManager.getLogger(name, GemFireParameterizedMessageFactory.INSTANCE));
  }

  /**
   * Returns a LogWriterLogger that is decorated with the LogWriter and LogWriterI18n
   * methods.
   * 
   * This is the bridge to LogWriter and LogWriterI18n that we need to eventually
   * stop using in phase 1. We will switch over from a shared LogWriterLogger instance
   * to having every GemFire class own its own private static GemFireLogger
   * 
   * @return The LogWriterLogger for the calling class.
   */
  public static LogWriterLogger createLogWriterLogger(final String name, final String connectionName, final boolean isSecure) {
    return LogWriterLogger.create(name, connectionName, isSecure);
  }
  
  /**
   * Return the Log4j Level associated with the int level.
   * 
   * @param intLevel
   *          The int value of the Level to return.
   * @return The Level.
   * @throws java.lang.IllegalArgumentException if the Level int is not registered.
   */
  public static Level toLevel(final int intLevel) {
    for (Level level : Level.values()) {
      if (level.intLevel() == intLevel) {
        return level;
      }
    }

    throw new IllegalArgumentException("Unknown int level [" + intLevel + "].");
  }

  /**
   * Gets the class name of the caller in the current stack at the given {@code depth}.
   *
   * @param depth a 0-based index in the current stack.
   * @return a class name
   */
  public static String getClassName(final int depth) {
    return new Throwable().getStackTrace()[depth].getClassName();
  }

  public static void configureFastLoggerDelegating() {
    final Configuration config = ((org.apache.logging.log4j.core.Logger)
        LogManager.getLogger(ROOT_LOGGER_NAME, GemFireParameterizedMessageFactory.INSTANCE)).getContext().getConfiguration();
    
    if (Configurator.hasContextWideFilter(config) || 
        Configurator.hasAppenderFilter(config) || 
        Configurator.hasDebugOrLower(config) || 
        Configurator.hasLoggerFilter(config) || 
        Configurator.hasAppenderRefFilter(config)) {
      FastLogger.setDelegating(true);
    } else {
      FastLogger.setDelegating(false);
    }
  }
  
  private static class PropertyChangeListenerImpl implements PropertyChangeListener {
    @SuppressWarnings("synthetic-access")
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      StatusLogger.getLogger().debug("LogService responding to a property change event. Property name is {}.",
          evt.getPropertyName());
      
      if (evt.getPropertyName().equals(LoggerContext.PROPERTY_CONFIG)) {
        configureFastLoggerDelegating();
      }
    }
  }
  
  public static void setBaseLogLevel(Level level) {
    if (isUsingGemFireDefaultConfig()) {
      Configurator.setLevel(ROOT_LOGGER_NAME, level);
    }
    Configurator.setLevel(BASE_LOGGER_NAME, level);
    Configurator.setLevel(MAIN_LOGGER_NAME, level);
  }
  
  public static void setSecurityLogLevel(Level level) {
    Configurator.setLevel(SECURITY_LOGGER_NAME, level);
  }
  
  public static Level getBaseLogLevel() {
    return Configurator.getLevel(BASE_LOGGER_NAME);
  }
  
  public static LoggerConfig getRootLoggerConfig() {
    //return ((org.apache.logging.log4j.core.Logger)LogService.getLogger()).getContext().getConfiguration().getLoggerConfig(LogManager.getRootLogger().getName());
    return Configurator.getLoggerConfig(LogManager.getRootLogger().getName());
  }
  
  /**
   * Removes STDOUT ConsoleAppender from ROOT logger. Only called when using
   * the log4j2-default.xml configuration. This is done when creating the
   * LogWriterAppender for log-file. The Appender instance is stored in 
   * stdoutAppender so it can be restored later using restoreConsoleAppender.
   */
  public static synchronized void removeConsoleAppender() {
    final AppenderContext appenderContext = LogService.getAppenderContext(LogService.ROOT_LOGGER_NAME);
    final LoggerConfig config = appenderContext.getLoggerConfig();
    Appender stdout = config.getAppenders().get(STDOUT);
    if (stdout != null) {
      config.removeAppender(STDOUT);
      stdoutAppender = stdout;
      appenderContext.getLoggerContext().updateLoggers();
    }
  }
  
  /**
   * Restores STDOUT ConsoleAppender to ROOT logger. Only called when using
   * the log4j2-default.xml configuration. This is done when the 
   * LogWriterAppender for log-file is destroyed. The Appender instance stored 
   * in stdoutAppender is used.
   */
  public static synchronized void restoreConsoleAppender() {
    if (stdoutAppender == null) {
      return;
    }
    final AppenderContext appenderContext = LogService.getAppenderContext(LogService.ROOT_LOGGER_NAME);
    final LoggerConfig config = appenderContext.getLoggerConfig();
    Appender stdout = config.getAppenders().get(STDOUT);
    if (stdout == null) {
      config.addAppender(stdoutAppender, Level.ALL, null);
      appenderContext.getLoggerContext().updateLoggers();
    }
  }
  
  private LogService() {
    // do not instantiate
  }
}
