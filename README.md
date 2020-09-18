# DNS-Standheizung

Keep caches of a recursive DNS Server filled with . NS records and A/AAAA

## Getting Started

```
git clone https://github.com/miesi/DNS-Standheizung
```

### Prerequisites

[Maven](https://maven.apache.org/)

[Java](http://openjdk.java.net/)

On debian based Systems
```
apt install maven openjdk-11-jre-headless
```

### Building

```
cd DNS-Standheizung/
mvn package
```

## Running

Starting with default values
```
java -cp target/dnsCacheWarmer-1.0-SNAPSHOT-jar-with-dependencies.jar de.mieslinger.dnscachewarmer.Main 
```

Preheat the local resolver
```
java -cp target/dnsCacheWarmer-1.0-SNAPSHOT-jar-with-dependencies.jar de.mieslinger.dnscachewarmer.Main -r 127.0.0.1
```

Help
```
java -cp target/dnsCacheWarmer-1.0-SNAPSHOT-jar-with-dependencies.jar de.mieslinger.dnscachewarmer.Main --help
```
## Authors

* **Thomas Mieslinger** 

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details



