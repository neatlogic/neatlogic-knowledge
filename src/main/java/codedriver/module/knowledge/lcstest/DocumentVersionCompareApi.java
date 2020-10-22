package codedriver.module.knowledge.lcstest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.BiPredicate;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.knowledge.dto.DocumentVo;
import codedriver.module.knowledge.dto.LineVo;
import codedriver.module.knowledge.lcstest.Node;
import codedriver.module.knowledge.lcstest.SegmentMapping;
import codedriver.module.knowledge.lcstest.SegmentRange;
//@Service
//@OperationType(type = OperationTypeEnum.SEARCH)
public class DocumentVersionCompareApi extends PrivateApiComponentBase {
    private final static String BASE_PATH = "src/main/java/codedriver/module/knowledge/lcstest/";
    @Override
    public String getToken() {
        return "knowledge/VersionCompara";
    }

    @Override
    public String getName() {
        return "比较两个版本文档内容差异";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({})
    @Output({
        @Param(explode = DocumentVo[].class, desc = "两个版本文档内容")
    })
    @Description(desc = "比较两个版本文档内容差异")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<DocumentVo> documentList = new ArrayList<>();
        DocumentVo oldDocument = new DocumentVo();
        DocumentVo newDocument = new DocumentVo();
        oldDocument.setTitle("oldTitle");
        newDocument.setTitle("newTitle");
//        List<LineVo> oldLineList = readFileData(BASE_PATH + "oldData.txt");
//        List<LineVo> newLineList = readFileData(BASE_PATH + "newData.txt");
        List<LineVo> oldLineList = initOldLineList();
        List<LineVo> newLineList = initNewLineList();
        List<LineVo> oldResultList = new ArrayList<>();
        List<LineVo> newResultList = new ArrayList<>();
        Node node = longestCommonSequence(oldLineList, newLineList, (e1, e2) -> e1.getContent().equals(e2.getContent()));
        for(SegmentMapping segmentMapping : node.getSegmentMappingList()) {
            test(oldLineList, newLineList, oldResultList, newResultList, segmentMapping);
        }
        oldDocument.setLineList(oldResultList);
        newDocument.setLineList(newResultList);
        documentList.add(oldDocument);
        documentList.add(newDocument);        
        return documentList;
    }
    
    private List<LineVo> initOldLineList(){
        List<LineVo> lineList = new ArrayList<>();
        int i = 0;
        lineList.add(new LineVo(++i, "h1", "配置 logback"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "我们开始通过多种配置 logback，以及许多示例的配置qq脚本。logback 依赖的配置ee框架 - Joran 将会在之后的章节介绍"));
        lineList.add(new LineVo(++i, "p", "在应用程序当中使用日志语句需要耗费大量的精力。根据调查，大约有百分之四的代码用于打印日志。即使在一个中型应用的代码当中也有成千上万条日志的打印语句。考虑到这种情况，我们需要使用工具来管理这些日志语句。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "可以通过编程或者配置 XML 脚本或者 Groovy 格式的方式来配置 logback。对于已经使用 log4j 的用户可以通过这个工具来把 log4j.properties 转换为 logback.xml。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "以下是 logback 的初始化步骤："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "logback 会在类路径下寻找名为 logback-test.xml 的文件。"));
        lineList.add(new LineVo(++i, "p", "如果没有找到，logback 会继续寻找名为 logback.groovy 的文件。"));
        lineList.add(new LineVo(++i, "p", "如果没有找到，logback 会继续寻找名为 logback.xml 的文件。"));        
        lineList.add(new LineVo(++i, "p", "如果没有找到，将会通过 JDK 提供的 ServiceLoader 工具在类路径下寻找文件 META-INFO/services/ch.qos.logback.classic.spi.Configurator，该文件的内容为实现了 Configurator 接口的实现类的全限定类名。"));
        lineList.add(new LineVo(++i, "p", "如果以上都没有成功，logback 会通过 BasicConfigurator 为自己进行配置，并且日志将会全部在控制台打印出来。"));
        lineList.add(new LineVo(++i, "p", "最后一步的目的是为了保证在所有的配置文件都没有被找到的情况下，提供一个默认的（但是是非常基础的）配置。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "如果你使用的是 maven，你可以在 src/test/resources 下新建 logback-test.xml。maven 会确保它不会被生成。所以你可以在测试环境中给配置文件命名为 logback-test.xml，在生产环境中命名为 logback.xml。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "FAST START-UP Joran 解析给定的配置文件大概需要耗费 100 毫秒。为了减少启动的世间安，你可以使用 ServiceLoader 来加载自定义的 Configurator，并使用 BasicConfigurator 作为一个好的起点（个人的理解是通过继承这个类）。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "自动配置 logback"));
        lineList.add(new LineVo(++i, "p", "最简单的方式配置 logback 是让它去加载默认的配置。例："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "假设配置文件 logback-test.xml 或者 logback.xml 不存在，logback 会调用 BasicConfigurator 进行最小的配置。最小的配置包含一个附加到 root logger 上的 ConsoleAppender，格式化输出使用 PatternLayoutEncoder 对模版 %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n 进行格式化。root logger 默认的日志级别为 DEBUG。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "所以，MyApp1 的输出信息如下："));
        lineList.add(new LineVo(++i, "p", "MyApp1 通过调用 org.slf4j.LoggerFactory 与 org.slf4j.Logger 这两个类与 logback 相关联，并检索会用到的 logger。除了配置 logback 的代码，客户端的代码不需要依赖 logback，因为 SLF4J 允许在它的抽象层下使用任何日志框架，所以非常容易将大量代码从一个框架迁移到另一个框架。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "使用 logback-test.xml 或 logback.xml 自动配置"));
        lineList.add(new LineVo(++i, "p", "下面的配置等同于通过 BasicConfigurator 进行配置。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "你需要将上面的配置文件命名为 logback.xml 或 logback-test.xml"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "运行 MyApp1，你将会看到相同的结果（你要是不相信，你可以更改模版，看是否生效）。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "在警告或错误的情况下自动打印状态信息"));
        lineList.add(new LineVo(++i, "p", "如果在解析配置文件的过程当中发生了错误，logback 会在控制台打印出它的内部状态数据。如果用户明确的定义了状态监听器，为了避免重复，logback 将不会自动打印状态信息。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "在没有警告或错误的情况下，如果你想查看 logback 内部的状态信息，可以通过 StatusPrinter 类来调用 print() 方法查看具体的信息。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "在 MyApp1 的基础上添加两行代码，并命名为 MyApp2"));
        lineList.add(new LineVo(++i, "p", "在输出信息中，可以清楚的看到内部的状态信息，又称之为 Status 对象，可以很方便的获取 logback 的内部状态。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "状态数据"));
        lineList.add(new LineVo(++i, "p", "你可以通过构造一个配置文件来打印状态信息，而不需要通过编码的方式调用 StatusPrinter 去实现。只需要在 configuration 元素上添加 debug 属性。配置文件如下所示。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "注意：debug 属性只跟状态信息有关，并不会影响 logback 的配置文件，也不会影响 logger 的日志级别。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "需要将 sample1.xml 改名为 logback.xml 或 logback-test.xml，不然 logbak 找不到配置文件。以后这种情况不再重复申明。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "如果配置文件的配置有问题，logback 会检测到这个错误并且在控制台打印它的内部状态。但是，如果配置文件没有被找到，logback 不会打印它的内部状态信息，因为没有检测到错误。通过编码方式调用 StatusPrinter.print() 方法会在任何情况下都打印状态信息。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "强制输出状态信息：在缺乏状态信息的情况下，要找一个有问题的配置文件很难，特别是在生产环境下。为了能够更好的定位到有问题的配置文件，可以通过系统属性 \"logback.statusListenerClass\" 来设置 StatusListener 强制输出状态信息。系统属性 \"logback.statusListenerClass\" 也可以用来在遇到错误的情况下进行输出。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "设置 debug=\"true\" 完全等同于配置一个 OnConsoleStatusListener 。具体示例如下："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example: onConsoleStatusListener.xml"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "设置 debug=\"true\" 与配置 OnConsoleStatusListener 的效果完全一样。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "通过系统属性指定默认的配置文件"));
        lineList.add(new LineVo(++i, "p", "通过系统属性 logback.configurationFile 可以指定默认的配置文件的路径。它的值可以是 URL，类路径下的文件或者是应用外部的文件。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "java -Dlogback.configurationFile=/path/to/config.xml chapters.configuration.MyApp1"));
        lineList.add(new LineVo(++i, "p", "注意：文件类型只能是 \".xml\" 或者 \".groovy\"，其它的拓展文件将会被忽略。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "因为 logback.configureFile 是一个系统属性，所以也可以在应用内进行设置。但是必须在 logger 实例创建前进行设置。"));
        lineList.add(new LineVo(++i, "h2", "当配置文件更改时，自动加载"));
        lineList.add(new LineVo(++i, "p", "为了让 logback 能够在配置文件改变的时候自动去扫描，需要在 <configuration> 标签上添加 scan=true 属性。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "默认情况下，一分钟扫描一次配置文件，看是否有更改。通过 <configuration> 标签上的 scanPeriod 属性可以指定扫描周期。扫描周期的时间单位可以是毫秒、秒、分钟或者小时。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "注意：如果没有指定时间单位，则默认为毫秒。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "当设置了 scan=\"true\"，会新建一个 ReconfigureOnChangeTask 任务用于监视配置文件是否变化。ReconfigureOnChangeTask 也会自动监视外部文件的变化。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "如果更改后的配置文件有语法错误，则会回退到之前的配置文件。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "在堆栈中展示包数据"));
        lineList.add(new LineVo(++i, "p", "注意：在 1.1.4 版本中，展示包数据是默认被禁用的。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "如果启用了展示包数据，logback 会在堆栈的每一行显示 jar 包的名字以及 jar 的版本号。展示包数据可以很好的解决 jar 版本冲突的问题。但是，这个的代价比较高，特别是在频繁报错的情况下。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example："));
        lineList.add(new LineVo(++i, "p", "启用展示包数据："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "直接调用 JoranConfigurator"));
        lineList.add(new LineVo(++i, "p", "Logback 依赖的配置文件库为 Joran，是 logback-core 的一部分。logback 的默认配置机制为：通过 JoranConfigurator 在类路径上寻找默认的配置文件。你可以通过直接调用 JoranConfigurator 的方式来重写 logback 的默认配置机制。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "查看内部状态信息"));
        lineList.add(new LineVo(++i, "p", "logback 通过 StatusManager 的对象来收集内部的状态信息，这个对象可以通过 LoggerContext 来获取。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "对于一个给定的 StatusManager，你可以获取 logback 上下文所有的状态信息。为了保持内存的使用在一个合理的水平，StatusManager 的默认实现包含两个部分：头部与尾部。头部存储第一个 H 状态的消息，尾部存储最后一个 T 状态的消息。目前 H=T=150，这个值在以后可能会改变。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "logback-classic 包含一个名叫 ViewStatusMessagesServlet 的 servlet。这个 servlet 打印当前 LoggerContext 的 StatusManager 的内容，通过 html 进行输出。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "监听状态信息"));
        lineList.add(new LineVo(++i, "p", "通过给 StatusManager 附加一个 StatusListener，可以对状态信息进行获取。特别是在配置好 logback 之后。注册一个状态监听器可以很方便的监听 logback 的内部状态，并且不需要人工的干预。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "StatusListener 有一个名为 OnConsoleStatusListener 的实现类，可以将状态信息在控制台打印出来。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "注意：注册的状态监听器只会获取注册之后产生的状态消息，而不会获取注册之前产生的消息。所以建议在最开始的时候直接进行配置。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "可以在配置文件中配置多个状态监听器。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "系统属性 \"logback.statusListenerClass\""));
        lineList.add(new LineVo(++i, "p", "通过设置 java 的系统属性来配置状态监听器。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "java -Dlogback.statusListenerClass=ch.qos.logback.core.status.OnConsoleStatusListener"));
        lineList.add(new LineVo(++i, "p", "logback 子级实现了几个监听器。OnConsoleStatusListener 用于在控制台打印状态消息。OnErrorConsoleStatusListener 用于在控制台打印显示错误的状态信息。NopStatusListener 会丢弃掉状态信息。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "注意：在配置期间，任何的状态监听器被注册，或者通过 java 系统变量指定 logback.statusListenerClass 的值，在警告或错误的情况下自动打印状态信息 将会被禁用。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "可以通过设置 java 系统变量 logback.statusListenerClass 的值来禁用一切状态信息的打印。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "java -Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener"));
        lineList.add(new LineVo(++i, "h2", "停止 logback-classic"));
        lineList.add(new LineVo(++i, "p", "为了释放 logback-classic 所使用的资源，停止使用 logger context 是一个好注意。停止 context 将会关闭所有在 logger 上定义的 appender，并且有序的停止正在活动的线程。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "import org.sflf4j.LoggerFactory;"));
        lineList.add(new LineVo(++i, "p", "import ch.qos.logback.classic.LoggerContext;"));
        lineList.add(new LineVo(++i, "p", "..."));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();"));
        lineList.add(new LineVo(++i, "p", "loggerContext.stop();"));
        lineList.add(new LineVo(++i, "p", "上面的代码在 web 应用中，通过调用 ServletContextListener 的 contextDestroyed) 方法来停止 logback-classic 并释放资源。1.1.10 版本后，ServletContextListener 会被自动加载。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "通过 shutddown hook 停止 logback-classic"));
        lineList.add(new LineVo(++i, "p", "个人觉得 hook 可以理解为钩子或者开关，但是还是觉得照写会更好理解一点。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "指定一个 JVM shutdown hook 可以非常方便的关闭 logback 并释放资源。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "<configuration debug=\"true\">"));
        lineList.add(new LineVo(++i, "p", "    <!-- 如果缺失 class 属性，则会默认加载 ch.qos.logback.core.hook.DefaultShutdownHook -->"));
        lineList.add(new LineVo(++i, "p", "    <shutdownHook/>"));
        lineList.add(new LineVo(++i, "p", "</configuration>"));
        lineList.add(new LineVo(++i, "p", "注意：可以通过 class 属性指定一个 shutdown hook 的名字。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "默认的 shutdown hook 为 DefaultShutdownHook，在一个指定的时间后（默认是 0）会停掉 context。但是允许 context 在 30s 内完成日志文件的打包。在独立的 java 应用程序中，在配置文件中添加 <shutdownHook/> 可以确保任何日志打包任务完成之后，JVM 才会退出。在 web 应用程序中，webShutdownHook 会自动安装，<shutdownHook/> 将会变的多余且没有必要。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "在 web 应用中使用 WebShutdownHook 停止 logback-classic"));
        lineList.add(new LineVo(++i, "p", "SINCE 1.1.10 logback-classic 会自动要求 web 服务安装 LogbackServletContainerInitializer（实现了 ServletContainerInitializer 接口，在 servlet-api 3.x 或以后的版本才有效）。这个初始化程序将会依次实例化 LogbackServletContextListener 的实例。在 web 应用停止或者重载的时候会停掉当前 logback-classic 的 context。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "我表示不是很懂这种做法有何意义，难道应用都停止了，context 还会在运行？这就是作者说的非常多余跟没有必要吗？"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "可以在 web.xml 中禁止 LogbackServletContextListener 的实例化。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "<web-app>"));
        lineList.add(new LineVo(++i, "p", "    <context-param>"));
        lineList.add(new LineVo(++i, "p", "        <param-name>logbackDisableServletContainerInitializer</param-name>"));
        lineList.add(new LineVo(++i, "p", "        <param-value>true</param-value>"));
        lineList.add(new LineVo(++i, "p", "    </context-param>"));
        lineList.add(new LineVo(++i, "p", "    ..."));
        lineList.add(new LineVo(++i, "p", "</web-app>"));
        lineList.add(new LineVo(++i, "p", "logbackDisableServletContainerInitializer 也可以通过 java 系统属性或者系统的环境变量来设置。优先级为：web 应用 > java 系统属性 > 系统环境变量"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h1", "配置文件的语法"));
        lineList.add(new LineVo(++i, "p", "logback 允许你重新定义日志的行为而不需要重新编译代码，你可以轻易的禁用调应用中某些部分的日志，或者将日志输出到任何地方。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "logback 的配置文件非常的灵活，不需要指定 DTD 或者 xml 文件需要的语法。但是，最基本的结构为 <configuration> 元素，包含 0 或多个 <appender> 元素，其后跟 0 或多个 <logger> 元素，其后再跟最多只能存在一个的 <root> 元素。基本结构图如下："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "标签名大小写敏感"));
        lineList.add(new LineVo(++i, "p", "在 logback 版本 0.9.17 之后，显示规定的标签名不区分大小写。例如：<logger>、<Logger、<LOGGER> 这些都是有效的标签名。xml 风格的规则仍然适用。如果你有一个开始标签为 <xyz>，那么必须要有一个结束标签 </xyz>。</XyZ> 则是错误的。根据默认规则，标签名字是大小写敏感的，除了第一个字母。所以，<xyz> 与 <Xyz> 是一样的，但是 <xYz> 是错误的。默认规则遵循驼峰命名法。很难说清楚一个标签遵循什么规则，如果你不知道给定的标签遵循哪种规则，那么使用驼峰命名法总是正确的。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "配置 logger"));
        lineList.add(new LineVo(++i, "p", "现在你至少应该对等级继承规则与基本规则有所了解.。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "通过 <logger> 标签来过 logger 进行配置，一个 <logger> 标签必须包含一个 name 属性，一个可选的 level 属性，一个可选 additivity 属性。additivity 的值为 true 或 false。level 的值为 TRACE，DEBUG，INFO，WARN，ERROR，ALL，OFF，INHERITED，NULL。当 level 的值为 INHERITED 或 NULL 时，将会强制 logger 继承上一层的级别。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "<logger> 元素至少包含 0 或多个 <appender-ref> 元素。每一个 appender 通过这种方式被添加到 logger 上。与 log4j 不同的是，logbakc-classic 不会关闭或移除任何之前在 logger 上定义好的的 appender。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "配置 root logger"));
        lineList.add(new LineVo(++i, "p", "root logger 通过 <root> 元素来进行配置。它只支持一个属性——level。它不允许设置其它任何的属性，因为 additivity 并不适用 root logger。而且，root logger 的名字已经被命名为 \"ROOT\"，也就是说也不支持 name 属性。level 属性的值可以为：TRACE、DEBUG、INFO、WARN、ERROR、ALL、OFF，但是不能设置为 INHERITED 或 NULL。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "跟 <logger 元素类似，<root> 元素可以包含 0 或多个 <appender-ref> 元素。"));
        return lineList;
    }
    
    private List<LineVo> initNewLineList(){
        List<LineVo> lineList = new ArrayList<>();
        int i = 0;
        lineList.add(new LineVo(++i, "h1", "配置 logback"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "我们开始通过多种配置 logback，以及许多gg示例的配置脚本。logback 依赖的配置框架 - Joran 将会在kk之后的章节介绍"));
        lineList.add(new LineVo(++i, "p", "在应用程序当中使用日志语句需要耗费大量的精力。根据调查，大约有百分之四的代码用于打印日志。即使在一个中型应用的代码当中也有成千上万条日志的打印语句。考虑到这种情况，我们需要使用工具来管理这些日志语句。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "可以通过编程或者配置 XML 脚本或者 Groovy 格式的方式来配置 logback。对于已经使用 log4j 的用户可以通过这个工具来把 log4j.properties 转换为 logback.xml。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "以下是 logback 的初始化步骤："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "logback 会在类路径下寻找名为 logback-test.xml 的文件。1"));
        lineList.add(new LineVo(++i, "p", "如果没有找到，logback 会继续寻找名为 logback.groovy 的文件。1"));
        lineList.add(new LineVo(++i, "p", "如果没有找到，logback 会继续寻找名为 logback.xml 的文件。1"));
//        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "如果没有找到，将会通过 JDK 提供的 ServiceLoader 工具在类路径下寻找文件 META-INFO/services/ch.qos.logback.classic.spi.Configurator，该文件的内容为实现了 Configurator 接口的实现类的全限定类名。"));
        lineList.add(new LineVo(++i, "p", "如果以上都没有成功，logback 会通过 BasicConfigurator 为自己进行配置，并且日志将会全部在控制台打印出来。"));
        lineList.add(new LineVo(++i, "p", "最后一步的目的是为了保证在所有的配置文件都没有被找到的情况下，提供一个默认的（但是是非常基础的）配置。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "如果你使用的是 maven，你可以在 src/test/resources 下新建 logback-test.xml。maven 会确保它不会被生成。所以你可以在测试环境中给配置文件命名为 logback-test.xml，在生产环境中命名为 logback.xml。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "FAST START-UP Joran 解析给定的配置文件大概需要耗费 100 毫秒。为了减少启动的世间安，你可以使用 ServiceLoader 来加载自定义的 Configurator，并使用 BasicConfigurator 作为一个好的起点（个人的理解是通过继承这个类）。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "自动配置 logback"));
        lineList.add(new LineVo(++i, "p", "最简单的方式配置 logback 是让它去加载默认的配置。例："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "假设配置文件 logback-test.xml 或者 logback.xml 不存在，logback 会调用 BasicConfigurator 进行最小的配置。最小的配置包含一个附加到 root logger 上的 ConsoleAppender，格式化输出使用 PatternLayoutEncoder 对模版 %d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n 进行格式化。root logger 默认的日志级别为 DEBUG。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "所以，MyApp1 的输出信息如下："));
        lineList.add(new LineVo(++i, "p", "MyApp1 通过调用 org.slf4j.LoggerFactory 与 org.slf4j.Logger 这两个类与 logback 相关联，并检索会用到的 logger。除了配置 logback 的代码，客户端的代码不需要依赖 logback，因为 SLF4J 允许在它的抽象层下使用任何日志框架，所以非常容易将大量代码从一个框架迁移到另一个框架。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "使用 logback-test.xml 或 logback.xml 自动配置"));
        lineList.add(new LineVo(++i, "p", "下面的配置等同于通过 BasicConfigurator 进行配置。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "你需要将上面的配置文件命名为 logback.xml 或 logback-test.xml"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "运行 MyApp1，你将会看到相同的结果（你要是不相信，你可以更改模版，看是否生效）。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "在警告或错误的情况下自动打印状态信息"));
        lineList.add(new LineVo(++i, "p", "如果在解析配置文件的过程当中发生了错误，logback 会在控制台打印出它的内部状态数据。如果用户明确的定义了状态监听器，为了避免重复，logback 将不会自动打印状态信息。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "在没有警告或错误的情况下，如果你想查看 logback 内部的状态信息，可以通过 StatusPrinter 类来调用 print() 方法查看具体的信息。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "在 MyApp1 的基础上添加两行代码，并命名为 MyApp2"));
        lineList.add(new LineVo(++i, "p", "在输出信息中，可以清楚的看到内部的状态信息，又称之为 Status 对象，可以很方便的获取 logback 的内部状态。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "状态数据"));
        lineList.add(new LineVo(++i, "p", "你可以通过构造一个配置文件来打印状态信息，而不需要通过编码的方式调用 StatusPrinter 去实现。只需要在 configuration 元素上添加 debug 属性。配置文件如下所示。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "注意：debug 属性只跟状态信息有关，并不会影响 logback 的配置文件，也不会影响 logger 的日志级别。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "需要将 sample1.xml 改名为 logback.xml 或 logback-test.xml，不然 logbak 找不到配置文件。以后这种情况不再重复申明。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "如果配置文件的配置有问题，logback 会检测到这个错误并且在控制台打印它的内部状态。但是，如果配置文件没有被找到，logback 不会打印它的内部状态信息，因为没有检测到错误。通过编码方式调用 StatusPrinter.print() 方法会在任何情况下都打印状态信息。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "强制输出状态信息：在缺乏状态信息的情况下，要找一个有问题的配置文件很难，特别是在生产环境下。为了能够更好的定位到有问题的配置文件，可以通过系统属性 \"logback.statusListenerClass\" 来设置 StatusListener 强制输出状态信息。系统属性 \"logback.statusListenerClass\" 也可以用来在遇到错误的情况下进行输出。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "设置 debug=\"true\" 完全等同于配置一个 OnConsoleStatusListener 。具体示例如下："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example: onConsoleStatusListener.xml"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "设置 debug=\"true\" 与配置 OnConsoleStatusListener 的效果完全一样。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "通过系统属性指定默认的配置文件"));
        lineList.add(new LineVo(++i, "p", "通过系统属性 logback.configurationFile 可以指定默认的配置文件的路径。它的值可以是 URL，类路径下的文件或者是应用外部的文件。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "java -Dlogback.configurationFile=/path/to/config.xml chapters.configuration.MyApp1"));
        lineList.add(new LineVo(++i, "p", "注意：文件类型只能是 \".xml\" 或者 \".groovy\"，其它的拓展文件将会被忽略。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "因为 logback.configureFile 是一个系统属性，所以也可以在应用内进行设置。但是必须在 logger 实例创建前进行设置。"));
        lineList.add(new LineVo(++i, "h2", "当配置文件更改时，自动加载"));
        lineList.add(new LineVo(++i, "p", "为了让 logback 能够在配置文件改变的时候自动去扫描，需要在 <configuration> 标签上添加 scan=true 属性。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "默认情况下，一分钟扫描一次配置文件，看是否有更改。通过 <configuration> 标签上的 scanPeriod 属性可以指定扫描周期。扫描周期的时间单位可以是毫秒、秒、分钟或者小时。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "注意：如果没有指定时间单位，则默认为毫秒。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "当设置了 scan=\"true\"，会新建一个 ReconfigureOnChangeTask 任务用于监视配置文件是否变化。ReconfigureOnChangeTask 也会自动监视外部文件的变化。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "如果更改后的配置文件有语法错误，则会回退到之前的配置文件。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "在堆栈中展示包数据"));
        lineList.add(new LineVo(++i, "p", "注意：在 1.1.4 版本中，展示包数据是默认被禁用的。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "如果启用了展示包数据，logback 会在堆栈的每一行显示 jar 包的名字以及 jar 的版本号。展示包数据可以很好的解决 jar 版本冲突的问题。但是，这个的代价比较高，特别是在频繁报错的情况下。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example："));
        lineList.add(new LineVo(++i, "p", "启用展示包数据："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "直接调用 JoranConfigurator"));
        lineList.add(new LineVo(++i, "p", "Logback 依赖的配置文件库为 Joran，是 logback-core 的一部分。logback 的默认配置机制为：通过 JoranConfigurator 在类路径上寻找默认的配置文件。你可以通过直接调用 JoranConfigurator 的方式来重写 logback 的默认配置机制。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "查看内部状态信息"));
        lineList.add(new LineVo(++i, "p", "logback 通过 StatusManager 的对象来收集内部的状态信息，这个对象可以通过 LoggerContext 来获取。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "对于一个给定的 StatusManager，你可以获取 logback 上下文所有的状态信息。为了保持内存的使用在一个合理的水平，StatusManager 的默认实现包含两个部分：头部与尾部。头部存储第一个 H 状态的消息，尾部存储最后一个 T 状态的消息。目前 H=T=150，这个值在以后可能会改变。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "logback-classic 包含一个名叫 ViewStatusMessagesServlet 的 servlet。这个 servlet 打印当前 LoggerContext 的 StatusManager 的内容，通过 html 进行输出。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "监听状态信息"));
        lineList.add(new LineVo(++i, "p", "通过给 StatusManager 附加一个 StatusListener，可以对状态信息进行获取。特别是在配置好 logback 之后。注册一个状态监听器可以很方便的监听 logback 的内部状态，并且不需要人工的干预。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "StatusListener 有一个名为 OnConsoleStatusListener 的实现类，可以将状态信息在控制台打印出来。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "注意：注册的状态监听器只会获取注册之后产生的状态消息，而不会获取注册之前产生的消息。所以建议在最开始的时候直接进行配置。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "可以在配置文件中配置多个状态监听器。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "系统属性 \"logback.statusListenerClass\""));
        lineList.add(new LineVo(++i, "p", "通过设置 java 的系统属性来配置状态监听器。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "java -Dlogback.statusListenerClass=ch.qos.logback.core.status.OnConsoleStatusListener"));
        lineList.add(new LineVo(++i, "p", "logback 子级实现了几个监听器。OnConsoleStatusListener 用于在控制台打印状态消息。OnErrorConsoleStatusListener 用于在控制台打印显示错误的状态信息。NopStatusListener 会丢弃掉状态信息。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "注意：在配置期间，任何的状态监听器被注册，或者通过 java 系统变量指定 logback.statusListenerClass 的值，在警告或错误的情况下自动打印状态信息 将会被禁用。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "可以通过设置 java 系统变量 logback.statusListenerClass 的值来禁用一切状态信息的打印。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "java -Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener"));
        lineList.add(new LineVo(++i, "h2", "停止 logback-classic"));
        lineList.add(new LineVo(++i, "p", "为了释放 logback-classic 所使用的资源，停止使用 logger context 是一个好注意。停止 context 将会关闭所有在 logger 上定义的 appender，并且有序的停止正在活动的线程。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "import org.sflf4j.LoggerFactory;"));
        lineList.add(new LineVo(++i, "p", "import ch.qos.logback.classic.LoggerContext;"));
        lineList.add(new LineVo(++i, "p", "..."));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();"));
        lineList.add(new LineVo(++i, "p", "loggerContext.stop();"));
        lineList.add(new LineVo(++i, "p", "上面的代码在 web 应用中，通过调用 ServletContextListener 的 contextDestroyed) 方法来停止 logback-classic 并释放资源。1.1.10 版本后，ServletContextListener 会被自动加载。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "通过 shutddown hook 停止 logback-classic"));
        lineList.add(new LineVo(++i, "p", "个人觉得 hook 可以理解为钩子或者开关，但是还是觉得照写会更好理解一点。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "指定一个 JVM shutdown hook 可以非常方便的关闭 logback 并释放资源。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "<configuration debug=\"true\">"));
        lineList.add(new LineVo(++i, "p", "    <!-- 如果缺失 class 属性，则会默认加载 ch.qos.logback.core.hook.DefaultShutdownHook -->"));
        lineList.add(new LineVo(++i, "p", "    <shutdownHook/>"));
        lineList.add(new LineVo(++i, "p", "</configuration>"));
        lineList.add(new LineVo(++i, "p", "注意：可以通过 class 属性指定一个 shutdown hook 的名字。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "默认的 shutdown hook 为 DefaultShutdownHook，在一个指定的时间后（默认是 0）会停掉 context。但是允许 context 在 30s 内完成日志文件的打包。在独立的 java 应用程序中，在配置文件中添加 <shutdownHook/> 可以确保任何日志打包任务完成之后，JVM 才会退出。在 web 应用程序中，webShutdownHook 会自动安装，<shutdownHook/> 将会变的多余且没有必要。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "在 web 应用中使用 WebShutdownHook 停止 logback-classic"));
        lineList.add(new LineVo(++i, "p", "SINCE 1.1.10 logback-classic 会自动要求 web 服务安装 LogbackServletContainerInitializer（实现了 ServletContainerInitializer 接口，在 servlet-api 3.x 或以后的版本才有效）。这个初始化程序将会依次实例化 LogbackServletContextListener 的实例。在 web 应用停止或者重载的时候会停掉当前 logback-classic 的 context。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "我表示不是很懂这种做法有何意义，难道应用都停止了，context 还会在运行？这就是作者说的非常多余跟没有必要吗？"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "可以在 web.xml 中禁止 LogbackServletContextListener 的实例化。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "Example："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "<web-app>"));
        lineList.add(new LineVo(++i, "p", "    <context-param>"));
        lineList.add(new LineVo(++i, "p", "        <param-name>logbackDisableServletContainerInitializer</param-name>"));
        lineList.add(new LineVo(++i, "p", "        <param-value>true</param-value>"));
        lineList.add(new LineVo(++i, "p", "    </context-param>"));
        lineList.add(new LineVo(++i, "p", "    ..."));
        lineList.add(new LineVo(++i, "p", "</web-app>"));
        lineList.add(new LineVo(++i, "p", "logbackDisableServletContainerInitializer 也可以通过 java 系统属性或者系统的环境变量来设置。优先级为：web 应用 > java 系统属性 > 系统环境变量"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h1", "配置文件的语法"));
        lineList.add(new LineVo(++i, "p", "logback 允许你重新定义日志的行为而不需要重新编译代码，你可以轻易的禁用调应用中某些部分的日志，或者将日志输出到任何地方。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "logback 的配置文件非常的灵活，不需要指定 DTD 或者 xml 文件需要的语法。但是，最基本的结构为 <configuration> 元素，包含 0 或多个 <appender> 元素，其后跟 0 或多个 <logger> 元素，其后再跟最多只能存在一个的 <root> 元素。基本结构图如下："));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "标签名大小写敏感"));
        lineList.add(new LineVo(++i, "p", "在 logback 版本 0.9.17 之后，显示规定的标签名不区分大小写。例如：<logger>、<Logger、<LOGGER> 这些都是有效的标签名。xml 风格的规则仍然适用。如果你有一个开始标签为 <xyz>，那么必须要有一个结束标签 </xyz>。</XyZ> 则是错误的。根据默认规则，标签名字是大小写敏感的，除了第一个字母。所以，<xyz> 与 <Xyz> 是一样的，但是 <xYz> 是错误的。默认规则遵循驼峰命名法。很难说清楚一个标签遵循什么规则，如果你不知道给定的标签遵循哪种规则，那么使用驼峰命名法总是正确的。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "配置 logger"));
        lineList.add(new LineVo(++i, "p", "现在你至少应该对等级继承规则与基本规则有所了解.。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "通过 <logger> 标签来过 logger 进行配置，一个 <logger> 标签必须包含一个 name 属性，一个可选的 level 属性，一个可选 additivity 属性。additivity 的值为 true 或 false。level 的值为 TRACE，DEBUG，INFO，WARN，ERROR，ALL，OFF，INHERITED，NULL。当 level 的值为 INHERITED 或 NULL 时，将会强制 logger 继承上一层的级别。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "<logger> 元素至少包含 0 或多个 <appender-ref> 元素。每一个 appender 通过这种方式被添加到 logger 上。与 log4j 不同的是，logbakc-classic 不会关闭或移除任何之前在 logger 上定义好的的 appender。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "h2", "配置 root logger"));
        lineList.add(new LineVo(++i, "p", "root logger 通过 <root> 元素来进行配置。它只支持一个属性——level。它不允许设置其它任何的属性，因为 additivity 并不适用 root logger。而且，root logger 的名字已经被命名为 \"ROOT\"，也就是说也不支持 name 属性。level 属性的值可以为：TRACE、DEBUG、INFO、WARN、ERROR、ALL、OFF，但是不能设置为 INHERITED 或 NULL。"));
        lineList.add(new LineVo(++i, "p", ""));
        lineList.add(new LineVo(++i, "p", "跟 <logger 元素类似，<root> 元素可以包含 0 或多个 <appender-ref> 元素。"));
        return lineList;
    }
    
    private static List<LineVo> readFileData(String filePath) {
        List<LineVo> lineList = new ArrayList<>();
        try (
            FileInputStream fis = new FileInputStream(filePath); 
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            ) {
            int lineNumber = 0;
            String str = null;
            while((str = br.readLine()) != null) {
                LineVo lineVo = new LineVo();
                lineVo.setContent(str);
                lineVo.setLineNumber(++lineNumber);
                lineVo.setType("p");
                System.out.println(lineVo);
                lineList.add(lineVo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lineList;
    }
    
    private static <T> Node longestCommonSequence(List<T> oldList, List<T> newList, BiPredicate<T, T> biPredicate) {
        Node[][] lcs = new Node[oldList.size()][newList.size()];       
        for(int i = 0; i < oldList.size(); i++) {
            for(int j = 0; j < newList.size(); j++) {
                Node currentNode = new Node(i, j);
                lcs[i][j] = currentNode;
                if(biPredicate.test(oldList.get(i), newList.get(j))) {
                    currentNode.setTotalMatchLength(1).setMatch(true);
                    Node upperLeftNode = null;
                    if(i > 0 && j > 0) {
                        upperLeftNode = lcs[i-1][j-1];
                    }
                    if(upperLeftNode != null) {
                        currentNode.setTotalMatchLength(upperLeftNode.getTotalMatchLength() + 1).setPrevious(upperLeftNode);
                    }
                }else {
                    int left = 0;
                    int top = 0;
                    Node leftNode = null;
                    if(j > 0) {
                        leftNode = lcs[i][j-1];
                    }
                    if(leftNode != null) {
                        left = leftNode.getTotalMatchLength();
                    }
                    Node topNode = null;
                    if(i > 0) {
                        topNode = lcs[i-1][j];
                    }
                    if(topNode != null) {
                        top = topNode.getTotalMatchLength();
                    }
                    if(top >= left) {
                        currentNode.setTotalMatchLength(top).setPrevious(topNode);
                    }else {
                        currentNode.setTotalMatchLength(left).setPrevious(leftNode);
                    }
                }
            }
        }       
        return lcs[oldList.size()-1][newList.size()-1];
    }
    
    private static void test(List<LineVo> oldDataList, List<LineVo> newDataList, List<LineVo> oldResultList, List<LineVo> newResultList, SegmentMapping segmentMapping) {
      SegmentRange oldSegmentRange = segmentMapping.getOldSegmentRange();
      SegmentRange newSegmentRange = segmentMapping.getNewSegmentRange();
      List<LineVo> oldSubList = oldDataList.subList(oldSegmentRange.getBeginIndex(), oldSegmentRange.getEndIndex());
      List<LineVo> newSubList = newDataList.subList(newSegmentRange.getBeginIndex(), newSegmentRange.getEndIndex());
      if(segmentMapping.isMatch()) {
          oldResultList.addAll(oldSubList);
          newResultList.addAll(newSubList);
      }else {
          if(CollectionUtils.isEmpty(newSubList)) {
              for(LineVo lineVo : oldSubList) {
                  lineVo.setChangeType("delete");
                  oldResultList.add(lineVo);
                  LineVo fillBlankLine = new LineVo();
                  fillBlankLine.setChangeType("fillblank");
                  fillBlankLine.setType(lineVo.getType());
                  newResultList.add(fillBlankLine);
              }
          }else if(CollectionUtils.isEmpty(oldSubList)) {
              for(LineVo lineVo :newSubList) {
                  LineVo fillBlankLine = new LineVo();
                  fillBlankLine.setChangeType("fillblank");
                  fillBlankLine.setType(lineVo.getType());
                  oldResultList.add(fillBlankLine);
                  lineVo.setChangeType("insert");
                  newResultList.add(lineVo);
              }
          }else if(oldSubList.size() == 1 && newSubList.size() == 1) {
              LineVo oldLine = oldSubList.get(0);
              LineVo newLine = newSubList.get(0);
              oldLine.setChangeType("delete");
              newLine.setChangeType("insert");
              if(StringUtils.length(oldLine.getContent()) > 0 && StringUtils.length(newLine.getContent()) > 0) {
                  List<SegmentRange> oldSegmentRangeList = new ArrayList<>();
                  List<SegmentRange> newSegmentRangeList = new ArrayList<>();
                  List<Character> oldCharList = new ArrayList<>();
                  for(char c : oldLine.getContent().toCharArray()) {
                      oldCharList.add(c);
                  }
                  List<Character> newCharList = new ArrayList<>();
                  for(char c : newLine.getContent().toCharArray()) {
                      newCharList.add(c);
                  }
                  Node node = longestCommonSequence(oldCharList, newCharList, (c1, c2) -> c1.equals(c2));
                  for(SegmentMapping segmentmapping : node.getSegmentMappingList()) {
                      oldSegmentRangeList.add(segmentmapping.getOldSegmentRange());
                      newSegmentRangeList.add(segmentmapping.getNewSegmentRange());
                  }
                  oldLine.setContent(wrapChangePlace(oldLine.getContent(), oldSegmentRangeList, "<span class='delete'>", "</span>"));
                  oldResultList.add(oldLine);
                  newLine.setContent(wrapChangePlace(newLine.getContent(), newSegmentRangeList, "<span class='insert'>", "</span>"));
                  newResultList.add(newLine);
              }else {
                  oldResultList.add(oldLine);
                  newResultList.add(newLine);
              }
          }else {
              List<SegmentMapping> segmentMappingList = longestCommonSequence2(oldSubList, newSubList);
              for(SegmentMapping segmentMap : segmentMappingList) {
                  test(oldSubList, newSubList, oldResultList, newResultList, segmentMap);
              }
          }
      }
    }
    
    private static String wrapChangePlace(String str, List<SegmentRange> segmentList, String startMark, String endMark) {
        int count = 0;
        for(SegmentRange segmentRange : segmentList) {
            if(!segmentRange.isMatch()) {
                count++;
            }
        }
        StringBuilder stringBuilder = new StringBuilder(str.length() + count * (startMark.length() + endMark.length()));
        for(SegmentRange segmentRange : segmentList) {
            if(segmentRange.getSize() > 0) {
                if(!segmentRange.isMatch()) {
                    stringBuilder.append(startMark);
                }
                stringBuilder.append(str.substring(segmentRange.getBeginIndex(), segmentRange.getEndIndex()));
                if(!segmentRange.isMatch()) {
                    stringBuilder.append(endMark);
                }
            }           
        }
        return stringBuilder.toString();
    }
    
    private static List<SegmentMapping> longestCommonSequence2(List<LineVo> oldList, List<LineVo> newList) {
        List<SegmentMapping> segmentMappingList = new ArrayList<>();
        List<Node> resultList = new ArrayList<>();
        PriorityQueue<Node> priorityQueue = new PriorityQueue<>(oldList.size() * newList.size(), (e1, e2) -> Integer.compare(e2.getTotalMatchLength(), e1.getTotalMatchLength()));
        for(int i = 0; i < oldList.size(); i++) {
            for(int j = 0; j < newList.size(); j++) {
                Node currentNode = new Node(i, j);
                LineVo oldStr = oldList.get(i);
                LineVo newStr = newList.get(j);
                int oldLineContentLength = StringUtils.length(oldStr.getContent());
                int newLineContentLength = StringUtils.length(newStr.getContent());
                if(oldLineContentLength == 0 || newLineContentLength == 0) {
                    currentNode.setTotalMatchLength(0);
                }else {
                    List<Character> oldCharList = new ArrayList<>();
                    for(char c : oldStr.getContent().toCharArray()) {
                        oldCharList.add(c);
                    }
                    List<Character> newCharList = new ArrayList<>();
                    for(char c : newStr.getContent().toCharArray()) {
                        newCharList.add(c);
                    }
                    Node node = longestCommonSequence(oldCharList, newCharList, (c1, c2) -> c1.equals(c2));
                    int maxLength = Math.max(oldLineContentLength, newLineContentLength);
                    int matchPercentage = (node.getTotalMatchLength() * 1000) / maxLength;
                    currentNode.setTotalMatchLength(matchPercentage);
                }
                priorityQueue.add(currentNode);
            }
        }
        Node e = null;
        while((e = priorityQueue.poll()) != null) {
            boolean flag = true;
            for(Node n : resultList) {
                if(n.getTotalMatchLength() == 0) {
                    flag = false;
                    break;
                }
                if(e.getOldIndex() >= n.getOldIndex() && e.getNewIndex() <= n.getNewIndex()) {
                    flag = false;
                    break;
                }
                if(e.getOldIndex() <= n.getOldIndex() && e.getNewIndex() >= n.getNewIndex()) {
                    flag = false;
                    break;
                }
            }
            if(flag) {
                resultList.add(e);           
            }
        }
        resultList.sort((e1, e2) -> Integer.compare(e1.getOldIndex(), e2.getOldIndex()));
        int oldIndex = 0;
        int newIndex = 0;
        for(Node node : resultList) {
            if(node.getOldIndex() > oldIndex) {
                SegmentMapping segmentMapping = new SegmentMapping(oldIndex, 0, false);
                segmentMapping.setEndIndex(node.getOldIndex(), 0);
                segmentMappingList.add(segmentMapping);
            }
            if(node.getNewIndex() > newIndex) {
                SegmentMapping segmentMapping = new SegmentMapping(0, newIndex, false);
                segmentMapping.setEndIndex(0, node.getNewIndex());
                segmentMappingList.add(segmentMapping);
            }
            oldIndex = node.getOldIndex() + 1;
            newIndex = node.getNewIndex() + 1;
            SegmentMapping segmentMapping = new SegmentMapping(node.getOldIndex(), node.getNewIndex(), false);
            segmentMapping.setEndIndex(oldIndex, newIndex);
            segmentMappingList.add(segmentMapping);
        }
        if(oldList.size() > oldIndex) {
            SegmentMapping segmentMapping = new SegmentMapping(oldIndex, 0, false);
            segmentMapping.setEndIndex(oldList.size(), 0);
            segmentMappingList.add(segmentMapping);
        }
        if(newList.size() > newIndex) {
            SegmentMapping segmentMapping = new SegmentMapping(0, newIndex, false);
            segmentMapping.setEndIndex(0, newList.size());
            segmentMappingList.add(segmentMapping);
        }
        return segmentMappingList;
    }
    
    public static void main(String[] args) {
        readFileData(BASE_PATH + "newData2.txt");
    }
}
