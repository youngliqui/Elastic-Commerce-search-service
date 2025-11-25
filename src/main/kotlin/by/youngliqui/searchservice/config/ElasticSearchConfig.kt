package by.youngliqui.searchservice.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@EnableElasticsearchRepositories(basePackages = ["by.youngliqui.searchservice.repository"])
class ElasticsearchConfig