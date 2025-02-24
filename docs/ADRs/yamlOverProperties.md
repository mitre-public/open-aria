# Why YAML configs are preferable to Properties

This page describes the trade-offs between using `java.util.Properties` vs. `yaml` for configuring Java applications.

**Bottom Line:** YAML is better.

**Protip:** Use YAML to create `Builders` or `Suppliers` that construct the class you need. Resist the temptation to
directly construct the "target class" via YAML parsing. Like all serialization frameworks, making a class compatible
with direct YAML parsing imposes design constraints. These constraints will be irrelevant sometimes and other times
completely block class creation. Consequently, make it standard practice to weave in a Builder or Supplier to take on
those design constraints. This will drastically simplify code when the "target class" is more complex than a "bag of
primitives POJO" [see more here](#advice-for-using-yaml-advice).

### Properties are better because...

- No dependency needs to be added the classpath
- Bugs are easy to fix because the "code path" is clear (i.e., no reflection magic)

### Properties are worse because...

- Bugs are more common.
- `Properties` do not natively support: integers, double, or lists.
- `Properties` have no structure. They are just a continuous spam of key-value pairs. 
- `Properties` require code that is littered with "key constants" (e.g. `public static final String NUM_THREADS = "num.threads";`)
    - This fragile boilerplate bloat code needs to be unit tested because these constants (and the methods that use them) frequently contain cut-paste errors.
- `Properties` lead to obfuscating important functionality with low level String parsing code.
- Raw `Properties` objects must be wrapped to obtain "nice" functionality like returning a `Duration` instead of an `int numSeconds` or validating configuration correctness (e.g. is property xyz set?)
- `Properties` cannot directly correspond to a Java POJO without extensive effort and numerous "key constants"
- `Properties` encourage a single "kitchen sink" style config file that becomes unwieldy over time.
- Special effort must be made to remove properties that have been "refactored away".  Without this effort old config files will contain multiple key-value pairs that do nothing but add confusion.
 
### YAML is better because...
- **YAML can directly translate to a Java POJO.**  
- Classes built to work with YAML are not obfuscated with numerous "key constants" (e.g. `public static final String NUM_THREADS = "num.threads";`).
- Classes built to work with YAML do not require extensive unit tests to verify the "key constants" are correctly wired.
- YAML natively supports more data types include Integers, Doubles, and Arrays
- YAML requires grouping fields into "sections" where each "section" corresponds to a Java POJO. This adds structure to the YAML file which makes the config easier to read.
- Raw YAML is easier for humans to read
- Refactoring a class built to work with YAML requires updating the .yaml file.  This ensures all config files are up-to-date and no vestigial fields are allowed.
- YAML encourages using Builders (i.e., YAML encourages good testable design)

### YAML is worse because...
- Using YAML configuration requires adding dependencies to the project (Jackson)
- The Jackson YAML parser performs "magic" via reflection so understanding how config objects get created takes experience.
- YAML is a serialization framework like Avro and GSON. The power of YAML serialization means the cleanest code you can create with YAML is better than the cleanest code you can create with `Properties`.  However, the ugliest code you can create with YAML is worse than the ugliest code you are likely to create with `Properties`.


### Advice for using YAML

1. YAML is great for replacing a list of command-line arguments with a simple text file like `config.yaml`.  The text file can directly correspond to a static helper class defined right beside the main method (i.e., `org.mitre.openaria.RunableProgram$ConfigOptions`).
2. It is better to create static helper classes (e.g., `ClassIWant.Builder` or `ClassIWant.Supplier`) with YAML than the actual "target class" (e.g. `ClassIWant`).
    - **Why:** Designing a class to be correctly built through YAML parsing imposes constraints on the implementation of the class.  Imposing these constraints on an ephemeral Builder or Supplier (rather than directly on `ClassIWant`) will **drastically** simplify code when creating complex classes. When creating simple classes this is a trivial extra step.
3. YAML is great for **powerful dependency injection that supports externally written code**.  Use this pattern to inject "Plugin Strategy Objects" from external code-bases.
    1. Declare an interface in your project you will expect others to implement on their own in some unknown external code-bases (e.g. `FileParser`, `OutputSink`, or `InterfaceMyAppUses`).
    2. Use YAML to name a class that implements `Supplier<InterfaceMyAppUses>`.  The Supplier layer is important because it makes life easier for our unknown external collaborators. Using a Supplier means our unknown collaborators can focus on correctly implementing  `InterfaceMyAppUses` in one class and push all "YAML compatibility requirements" to the class that implements `Supplier<InterfaceMyAppUses>`.
    3. Next, Use YAML to create the `Supplier`.  Then call the Supplier's `.get()` method.  This pattern allows the injected Supplier class to call a regular old constructor AND it allows the implementation of `InterfaceMyAppUses` to ignore all "Yaml  compatibility concerns".
    
4. Learn how to use `PluginFactory` and `YamlConfigured` to create Suppliers that provide "Plugin Strategy Objects" from external code-bases.  For example, ARIA code needs to send its output "somewhere" (see `OutputSink` interface).  The OutputSink used for a particular execution of ARIA is a configurable component.  It is important for ARIA to support "injecting" OutputSinks that are defined outside the ARIA codebase.  See `org.mitre.openaria.airborne.OutputConfig.Builder` for an example of injecting unknown OutputSinks by creating objects that implements `Supplier<OutputSink<AirborneEvent>>`.


```
//Example of how to write an "Injectable Strategy Object" that implements an important interface


/** This first example is simple enough that create a "static InnerSupplier" feels unnecessary. */
public class SimpleClass implements ImportantInterface {
	 
  // Easy class implementation with no-arg constructor goes here...	 
	 
   
  public static class InnerSupplier implements Supplier<SomeClass> {
    //This helper class feels unnecessary when the plugin is simple ... but using a Supplier is a good habit for when the plugin is complex.
  
    public SomeClass get() {
      return new SimpleClass();
    }
  }
}



/** 
 * This second example is complex so the "static InnerSupplier" performs 
 * a major service to code clarity and separation of concerns. 
 */
public class ComplexClass implements ImportantInterface {
	 
  // Standard class implementation goes here ...
  // The design of ComplexClass is not impacted by needs of YAML parsing..
  // Simply use concrete constructors
  // Use Immutable state
  // Directly unit test class with no special tooling 


  public static class InnerSupplier implements Supplier<ComplexClass>, YamlConfiged {
    // Here the "standard pattern" of using the helper class pays off big time.
    //
    // The YAML helper class allows ComplexClass to "only focus on implementing ImportantInterface".
    // The YAML helper class takes on all the design restrictions YAML parsing imposes, thus simplifying ComplexClass

    String fieldA;
    Integer fieldB;
    Boolean fieldC;

    //  See also:  YamlConfiged & InjectedClassFactory
    @Override    
    public void configure(Map<String, ?> configs) {
      System.out.println("Applying Configuration to a ComplexClass$InnerSupplier");
      
      requireMapKeys(configs, "fieldA", "fieldB", "fieldC");  
      this.fieldA = (String) configs.get("fieldA");
      this.fieldB = (Integer) configs.get("fieldB");
      this.fieldC = (Boolean) configs.get("fieldC");
    }
    
    public SomeClass get() {
      requireNonNull(fieldA);
      requireNonNull(fieldB);
      requireNonNull(fieldC);

      //Use data extracted from YAML to do something harder...

      return (fieldC)
          ? new ComplexClass(fieldA, fieldB)
          : new ComplexClass(fieldA);
    }
  }
}


//using in YAML via...
---
pluginSupplier:
  - pluginClass: org.unknown.collaborator.SimpleClass$InnerSupplier
  
---
pluginSupplier:
  - pluginClass: org.unknown.collaborator.ComplexClass$InnerSupplier
    configOptions:
      fieldA: "I am an important config String"
      fieldB: 42
      fieldC: true
```
   