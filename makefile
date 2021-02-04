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

scan: classes
	mkdir -p output
	$(JVM) $(MAIN) test/test0.txt > output/output0.txt

test: classes
	mkdir -p output
	$(JVM) $(MAIN) test/test1.txt > output/output1.txt
	$(JVM) $(MAIN) test/test2.txt > output/output2.txt
	$(JVM) $(MAIN) test/test3.txt > output/output3.txt
	$(JVM) $(MAIN) test/test4.txt > output/output4.txt
	$(JVM) $(MAIN) test/test5.txt > output/output5.txt
	$(JVM) $(MAIN) test/test6.txt > output/output6.txt
	$(JVM) $(MAIN) test/test7.txt > output/output7.txt
	$(JVM) $(MAIN) test/test8.txt > output/output8.txt
	$(JVM) $(MAIN) test/test9.txt > output/output9.txt
	$(JVM) $(MAIN) test/test10.txt > output/output10.txt

tool: $(TOOLPATH)/*.java
	$(JC) $(JFLAG) . $^
	$(JVM) $(TOOL) $(SRCPATH)

clean:
	$(RM) $(RMFLAG) com