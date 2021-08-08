# MavenPlugin
## 说明
一个自用的maven插件，用于代码自动生成
## 使用参考
- 先执行本项目的 mvn install
- 在其他项目的pom.xml中添加以下配置
```xml
<build>
        <plugins>
            <plugin>
                <groupId>com.ceragon</groupId>
                <artifactId>table-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <configuration>
                    <tableSourceDir>${project.basedir}/src/main/resources/model</tableSourceDir>
                    <outputDirectory>${project.basedir}/src/main/java</outputDirectory>
                </configuration>
            </plugin>

        </plugins>
    </build>
```
## 执行
```shell
mvn table:generate
```
