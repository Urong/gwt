/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.dev.codeserver;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.ArgProcessorBase;
import com.google.gwt.dev.util.arg.ArgHandlerLogLevel;
import com.google.gwt.dev.util.arg.ArgHandlerSourceLevel;
import com.google.gwt.dev.util.arg.OptionLogLevel;
import com.google.gwt.dev.util.arg.OptionSourceLevel;
import com.google.gwt.dev.util.arg.SourceLevel;
import com.google.gwt.util.tools.ArgHandler;
import com.google.gwt.util.tools.ArgHandlerDir;
import com.google.gwt.util.tools.ArgHandlerExtra;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerInt;
import com.google.gwt.util.tools.ArgHandlerString;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines the command-line options for the {@link CodeServer CodeServer's} main() method.
 *
 * <p>These flags are EXPERIMENTAL and subject to change.</p>
 */
public class Options {
  private boolean compileIncremental = false;
  private boolean noPrecompile = false;
  private boolean isCompileTest = false;
  private File workDir;
  private List<String> moduleNames = new ArrayList<String>();
  private boolean allowMissingSourceDir = false;
  private final List<File> sourcePath = new ArrayList<File>();
  private String bindAddress = "127.0.0.1";
  private String preferredHost = "localhost";
  private int port = 9876;
  private RecompileListener recompileListener = RecompileListener.NONE;
  private TreeLogger.Type logLevel = TreeLogger.Type.INFO;
  // Use the same default as the GWT compiler.
  private SourceLevel sourceLevel = SourceLevel.DEFAULT_SOURCE_LEVEL;
  private boolean failOnError = false;
  private boolean strictResources = false;
  private int compileTestRecompiles = 0;

  /**
   * Sets each option to the appropriate value, based on command-line arguments.
   * If there is an error, prints error messages and/or usage to System.err.
   * @return true if the arguments were parsed successfully.
   */
  public boolean parseArgs(String[] args) {
    boolean ok = new ArgProcessor().processArgs(args);
    if (!ok) {
      return false;
    }

    if (isCompileTest && noPrecompile) {
      System.err.println("Usage: -noprecompile and -compiletest are incompatible");
      return false;
    }

    if (moduleNames.isEmpty()) {
      System.err.println("Usage: at least one module must be supplied");
      return false;
    }

    return true;
  }

  /**
   * A Java application that embeds Super Dev Mode can use this hook to find out
   * when compiles start and end.
   */
  public void setRecompileListener(RecompileListener recompileListener) {
    this.recompileListener = recompileListener;
  }

  RecompileListener getRecompileListener() {
    return recompileListener;
  }

  /**
   * The top level of the directory tree where the code server keeps compiler output.
   */
  File getWorkDir() {
    return workDir;
  }

  /**
   * The names of the module that will be compiled (along with all its dependencies).
   */
  List<String> getModuleNames() {
    return moduleNames;
  }

  /**
   * Whether the codeServer should allow missing source directories.
   */
  boolean shouldAllowMissingSourceDir() {
    return allowMissingSourceDir;
  }

  /**
   * Whether to compile a series of reusable libraries that are linked at the end.
   */
  boolean shouldCompileIncremental() {
    return compileIncremental;
  }

  /**
   * Whether the codeServer should start without precompiling modules.
   */
  boolean getNoPrecompile() {
    return noPrecompile;
  }

  /**
   * The tree logger level.
   */
  TreeLogger.Type getLogLevel() {
    return logLevel;
  }

  /**
   * Java source level compatibility,
   */
  SourceLevel getSourceLevel() {
    return sourceLevel;
  }

  /**
   * If true, just compile the modules, then exit.
   */
  boolean isCompileTest() {
    return isCompileTest;
  }

  /**
   * The IP address where the code server should listen.
   */
  String getBindAddress() {
    return bindAddress;
  }

  int getCompileTestRecompiles() {
    return compileTestRecompiles;
  }

  /**
   * The hostname to put in a URL pointing to the code server.
   */
  String getPreferredHost() {
    return preferredHost;
  }

  /**
   * The port where the code server will listen for HTTP requests.
   */
  int getPort() {
    return port;
  }

  /**
   * Whether to implicitly import client and public directories when no explicit imports exist.
   */
  boolean enforceStrictResources() {
    return strictResources;
  }

  List<File> getSourcePath() {
    return sourcePath;
  }

  /**
   * If true, run the compiler in "strict" mode, which fails the compile if any Java file
   * cannot be compiled, whether or not it is used.
   */
  boolean isFailOnError() {
    return failOnError;
  }

  private class ArgProcessor extends ArgProcessorBase {

    public ArgProcessor() {
      registerHandler(new NoPrecompileFlag());
      registerHandler(new CompileTestFlag());
      registerHandler(new CompileTestRecompilesFlag());
      registerHandler(new BindAddressFlag());
      registerHandler(new PortFlag());
      registerHandler(new WorkDirFlag());
      registerHandler(new AllowMissingSourceDirFlag());
      registerHandler(new SourceFlag());
      registerHandler(new ModuleNameArgument());
      registerHandler(new FailOnErrorFlag());
      registerHandler(new StrictResourcesFlag());
      registerHandler(new CompileIncrementalFlag());
      registerHandler(new ArgHandlerSourceLevel(new OptionSourceLevel() {
        @Override
        public SourceLevel getSourceLevel() {
          return sourceLevel;
        }

        @Override
        public void setSourceLevel(SourceLevel sourceLevel) {
          Options.this.sourceLevel = sourceLevel;
        }
      }));
      registerHandler(new ArgHandlerLogLevel(new OptionLogLevel() {
        @Override
        public TreeLogger.Type getLogLevel() {
          return logLevel;
        }

        @Override
        public void setLogLevel(TreeLogger.Type logLevel) {
          Options.this.logLevel = logLevel;
        }
      }));
    }

    @Override
    protected String getName() {
      return CodeServer.class.getName();
    }
  }

  private class NoPrecompileFlag extends ArgHandlerFlag {

    @Override
    public String getLabel() {
      return "precompile";
    }

    @Override
    public String getPurposeSnippet() {
      return "Precompile modules.";
    }

    @Override
    public boolean setFlag(boolean value) {
      noPrecompile = !value;
      return true;
    }

    @Override
    public boolean getDefaultValue() {
      return !noPrecompile;
    }
  }

  private class CompileIncrementalFlag extends ArgHandlerFlag {

    @Override
    public String getLabel() {
      return "incremental";
    }

    @Override
    public String getPurposeSnippet() {
      return "Compile and link the application as a set of separate libraries.";
    }

    @Override
    public boolean setFlag(boolean value) {
      compileIncremental = value;
      return true;
    }

    @Override
    public boolean getDefaultValue() {
      return false;
    }

    @Override
    public boolean isExperimental() {
      return true;
    }
  }

  private class CompileTestFlag extends ArgHandlerFlag {

    @Override
    public String getLabel() {
      return "compileTest";
    }

    @Override
    public String getPurposeSnippet() {
      return "Exits after compiling the modules. The exit code will be 0 if the compile succeeded.";
    }

    @Override
    public boolean setFlag(boolean value) {
      isCompileTest = value;
      return true;
    }

    @Override
    public boolean getDefaultValue() {
      return isCompileTest;
    }
  }

  private class CompileTestRecompilesFlag extends ArgHandlerInt {

    @Override
    public String getTag() {
      return "-compileTestRecompiles";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] { "count" };
    }

    @Override
    public String getPurpose() {
      return "The number of times to recompile (after the first one) during a compile test.";
    }

    @Override
    public void setInt(int value) {
      compileTestRecompiles = value;
    }
  }

  private class BindAddressFlag extends ArgHandlerString {

    @Override
    public String getTag() {
      return "-bindAddress";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"address"};
    }

    @Override
    public String getPurpose() {
      return "The ip address of the code server. Defaults to 127.0.0.1.";
    }

    @Override
    public boolean setString(String newValue) {
      try {
        InetAddress newBindAddress = InetAddress.getByName(newValue);
        if (newBindAddress.isAnyLocalAddress()) {
          preferredHost = InetAddress.getLocalHost().getHostName();
        } else {
          preferredHost = newValue;
        }
      } catch (UnknownHostException e) {
        System.err.println("Can't resolve bind address: " + newValue);
        return false;
      }

      // Save the original since there's no way to get it back from an InetAddress.
      bindAddress = newValue;
      return true;
    }
  }

  private class PortFlag extends ArgHandlerInt {

    @Override
    public String getTag() {
      return "-port";
    }

    @Override
    public String[] getTagArgs() {
      return new String[] {"port"};
    }


    @Override
    public String getPurpose() {
      return "The port where the code server will run.";
    }

    @Override
    public void setInt(int newValue) {
      port = newValue;
    }
  }

  private class WorkDirFlag extends ArgHandlerDir {

    @Override
    public String getTag() {
      return "-workDir";
    }

    @Override
    public String getPurpose() {
      return "The root of the directory tree where the code server will"
          + "write compiler output. If not supplied, a temporary directory"
          + "will be used.";
    }

    @Override
    public void setDir(File newValue) {
      workDir = newValue;
    }
  }

  private class FailOnErrorFlag extends ArgHandlerFlag {

    FailOnErrorFlag() {
      // Backward compatibility with -strict in the regular compiler.
      addTagValue("-strict", true);
    }

    @Override
    public String getLabel() {
      return "failOnError";
    }

    @Override
    public boolean getDefaultValue() {
      return false;
    }

    @Override
    public String getPurposeSnippet() {
      return "Stop compiling if a module has a Java file with a compile error, even if unused.";
    }

    @Override
    public boolean setFlag(boolean value) {
      failOnError = value;
      return true;
    }
  }

  private class StrictResourcesFlag extends ArgHandlerFlag {

    public StrictResourcesFlag() {
      addTagValue("-XstrictResources", true);
    }

    @Override
    public boolean isExperimental() {
      return true;
    }

    @Override
    public String getLabel() {
      return "enforceStrictResources";
    }

    @Override
    public String getPurposeSnippet() {
      return "Don't implicitly depend on \"client\" and \"public\" when "
          + "a module doesn't define any dependencies.";
    }

    @Override
    public boolean setFlag(boolean value) {
      strictResources = value;
      return true;
    }

    @Override
    public boolean getDefaultValue() {
      return false;
    }
  }

  private class AllowMissingSourceDirFlag extends ArgHandlerFlag {

    @Override
    public String getLabel() {
      return "allowMissingSrc";
    }

    @Override
    public String getPurposeSnippet() {
      return "Allows -src flags to reference missing directories.";
    }

    @Override
    public boolean setFlag(boolean value) {
      allowMissingSourceDir = value;
      return true;
    }

    @Override
    public boolean getDefaultValue() {
      return allowMissingSourceDir;
    }
  }

  private class SourceFlag extends ArgHandler {

    @Override
    public String getTag() {
      return "-src";
    }

    @Override
    public String[] getTagArgs() {
      return new String[]{"dir"};
    }

    @Override
    public String getPurpose() {
      return "A directory containing GWT source to be prepended to the classpath for compiling.";
    }

    @Override
    public int handle(String[] args, int startIndex) {
      if (startIndex + 1 >= args.length) {
        System.err.println(getTag() + " should be followed by the name of a directory");
        return -1;
      }

      File candidate = new File(args[startIndex + 1]);
      if (!allowMissingSourceDir && !candidate.isDirectory()) {
        System.err.println("not a directory: " + candidate);
        return -1;
      }

      sourcePath.add(candidate);
      return 1;
    }
  }

  private class ModuleNameArgument extends ArgHandlerExtra {

    @Override
    public String[] getTagArgs() {
      return new String[] {"module"};
    }

    @Override
    public String getPurpose() {
      return "The GWT modules that the code server should compile. (Example: com.example.MyApp)";
    }

    @Override
    public boolean addExtraArg(String arg) {
      moduleNames.add(arg);
      return true;
    }
  }
}
