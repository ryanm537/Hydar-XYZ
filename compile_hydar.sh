javac -cp "./classes/" -d "./classes" ./src/java/xyz/hydar/*.java
java --enable-preview -cp "./classes/;./lib/*" xyz.hydar.Hydar
