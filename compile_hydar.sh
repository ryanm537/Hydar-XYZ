javac -cp "./classes/" -d "./classes" ./src/java/xyz/hydar/ee/*.java ./src/java/xyz/hydar/turn/*.java ./src/java/xyz/hydar/app/*.java
java --enable-preview -cp "./classes/:./lib/*" xyz.hydar.ee.Hydar