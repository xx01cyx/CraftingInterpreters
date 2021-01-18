JFLAG = -d
JC = javac
JVM = java

SRCPATH = src/com/craftinginterpreters/lox
TOOLPATH = src/com/craftinginterpreters/tool
OUTPATH = out/production/CraftingInterpreters
MAIN = com.craftinginterpreters.lox.Lox
TOOL = com.craftinginterpreters.tool.GenerateAst

RMFLAG = -r


classes: $(SRCPATH)/*.java
	$(JC) $(JFLAG) . $^

run: classes
	$(JVM) $(MAIN)

test: classes
	$(JVM) $(MAIN) test/test1.txt > output/output1.txt

tool: $(TOOLPATH)/*.java
	$(JC) $(JFLAG) . $^
	$(JVM) $(TOOL) $(SRCPATH)

clean:
	$(RM) $(RMFLAG) com