JFLAG = -d
JC = javac
JVM = java
SRCPATH = src/com/craftinginterpreters/lox
OUTPATH = out/production/CraftingInterpreters
MAIN = com.craftinginterpreters.lox.Lox
RMFLAG = -r

classes: $(SRCPATH)/*.java
	$(JC) $(JFLAG) . $^

run: classes
	$(JVM) $(MAIN)

test: classes
	$(JVM) $(MAIN) test/test1.txt > output/output1.txt

clean:
	$(RM) $(RMFLAG) com