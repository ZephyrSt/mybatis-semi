//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package top.zephyrs.mybatis.semi.spring.boot.autoconfigure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.ibatis.io.VFS;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;

public class SpringBootVFS extends VFS {
    private static Charset urlDecodingCharset;
    private static Supplier<ClassLoader> classLoaderSupplier;
    private final ResourcePatternResolver resourceResolver;

    public SpringBootVFS() {
        this.resourceResolver = new PathMatchingResourcePatternResolver((ClassLoader)classLoaderSupplier.get());
    }

    public boolean isValid() {
        return true;
    }

    protected List<String> list(URL url, String path) throws IOException {
        String urlString = URLDecoder.decode(url.toString(), urlDecodingCharset.name());
        String baseUrlString = urlString.endsWith("/") ? urlString : urlString.concat("/");
        Resource[] resources = this.resourceResolver.getResources(baseUrlString + "**/*.class");
        return (List)Stream.of(resources).map((resource) -> {
            return preserveSubpackageName(baseUrlString, resource, path);
        }).collect(Collectors.toList());
    }

    public static void setUrlDecodingCharset(Charset charset) {
        urlDecodingCharset = charset;
    }

    public static void setClassLoaderSupplier(Supplier<ClassLoader> supplier) {
        classLoaderSupplier = supplier;
    }

    private static String preserveSubpackageName(String baseUrlString, Resource resource, String rootPath) {
        try {
            return rootPath + (rootPath.endsWith("/") ? "" : "/") + resource.getURL().toString().substring(baseUrlString.length());
        } catch (IOException var4) {
            throw new UncheckedIOException(var4);
        }
    }

    static {
        setUrlDecodingCharset(Charset.defaultCharset());
        setClassLoaderSupplier(ClassUtils::getDefaultClassLoader);
    }
}
