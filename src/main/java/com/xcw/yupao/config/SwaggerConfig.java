/*
package com.xcw.yupao.config;

import io.swagger.annotations.Api;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket docket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                //这里一定要标注你控制器的位置
                //.apis(RequestHandlerSelectors.basePackage("com.xcw.yupao.controller.UserController"))
                .apis(RequestHandlerSelectors.withClassAnnotation(Api.class))
                .paths(PathSelectors.any())
                .build();
    }

    */
/**
     * api 信息
     * @return
     *//*

    private ApiInfo apiInfo(){
        return new ApiInfoBuilder()
                .title("鱼皮用户中心")
                .description("鱼皮用户中心文档")
                .termsOfServiceUrl("https://github.com/liyupi")
                .contact(new Contact("yupi","https://github.com/liyupi","1212121@qq.com"))
                .version("1.0")
                .build();
    }

}
*/
