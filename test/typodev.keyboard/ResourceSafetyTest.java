package typodev.keyboard;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import static org.junit.Assert.*;

public class ResourceSafetyTest
{
  @Test public void baseResourcesNeverReferenceNightOnlyColors() throws Exception
  {
    Set<String> colors = new HashSet<>();
    try (Stream<Path> files = Files.list(Paths.get("res/values")))
    {
      for (Path file : (Iterable<Path>)files.filter(p -> p.toString().endsWith(".xml"))::iterator)
      {
        NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(file.toFile()).getElementsByTagName("color");
        for (int i = 0; i < nodes.getLength(); i++)
          colors.add(((Element)nodes.item(i)).getAttribute("name"));
      }
    }
    Path colorDir = Paths.get("res/color");
    if (Files.isDirectory(colorDir))
      try (Stream<Path> files = Files.list(colorDir))
      {
        files.forEach(p -> colors.add(p.getFileName().toString().replaceFirst("\\.xml$", "")));
      }
    Pattern ref = Pattern.compile("@color/([A-Za-z0-9_]+)");
    try (Stream<Path> files = Files.walk(Paths.get("res")))
    {
      for (Path file : (Iterable<Path>)files.filter(p -> p.toString().endsWith(".xml"))::iterator)
      {
        if (file.getParent().getFileName().toString().contains("-")) continue;
        Matcher matcher = ref.matcher(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
        while (matcher.find())
          assertTrue(file + " references a color without a default: " + matcher.group(1),
              colors.contains(matcher.group(1)));
      }
    }
  }

  @Test public void manifestAndLayoutsReferenceCompiledClasses() throws Exception
  {
    Set<String> classes = new HashSet<>();
    Pattern appClass = Pattern.compile("typodev\\.keyboard(?:\\.[A-Za-z_$][A-Za-z0-9_$]*)+");
    Set<Path> files = new HashSet<>();
    files.add(Paths.get("AndroidManifest.xml"));
    try (Stream<Path> resources = Files.walk(Paths.get("res")))
    {
      resources.filter(p -> p.toString().endsWith(".xml")).forEach(files::add);
    }
    for (Path file : files)
    {
      Matcher matcher = appClass.matcher(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
      while (matcher.find()) classes.add(matcher.group());
    }
    assertFalse(classes.isEmpty());
    for (String name : classes)
      assertNotNull(Class.forName(name, false, getClass().getClassLoader()));
  }
}
