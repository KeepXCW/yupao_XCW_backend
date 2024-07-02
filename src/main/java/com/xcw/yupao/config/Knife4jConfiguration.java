package com.xcw.yupao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@Configuration
@EnableSwagger2WebMvc
@Profile({"dev", "test"})//代表只有dev、test环境下，swagger文档才能被访问
public class Knife4jConfiguration {

   /* @Bean(value = "defaultApi2")
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 这里一定要标注你控制器的位置
                .apis(RequestHandlerSelectors.basePackage("com.xcw.yupao.controller"))
                .paths(PathSelectors.any())
                .build();
    }*/

    /**
     * api 信息
     * @return
     */
    /*private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("鱼皮用户中心")
                .description("鱼皮用户中心接口文档")
                .termsOfServiceUrl("https://github.com/liyupi")
                .contact(new Contact("yupi","https://github.com/liyupi","xxx@qq.com"))
                .version("1.0")
                .build();
    }*/


    /*写法二*/
    @Bean(value = "dockerBean")
    public Docket dockerBean() {
        //指定使用Swagger2规范
        Docket docket=new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                //描述字段支持Markdown语法
                .description("# 伙伴匹配接口文档")
                .termsOfServiceUrl("https://doc.xiaominfo.com/")
                .contact("xiaoymin@foxmail.com")
                .version("1.0")
                .build())
                //分组名称
                .groupName("用户服务")
                .select()
                //这里指定Controller扫描包路径
                .apis(RequestHandlerSelectors.basePackage("com.xcw.yupao.controller"))
                .paths(PathSelectors.any())
                .build();
        return docket;
    }
}
