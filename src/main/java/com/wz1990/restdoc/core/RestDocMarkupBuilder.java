package com.wz1990.restdoc.core;

import com.wz1990.restdoc.helper.AttributeAsciidocBuilder;
import com.wz1990.restdoc.schema.Group;
import com.wz1990.restdoc.schema.Item;
import com.wz1990.restdoc.schema.Node;
import com.wz1990.restdoc.schema.Tree;
import io.github.swagger2markup.markup.builder.MarkupBlockStyle;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RestDocMarkupBuilder {

    Tree tree;
    String path;
    AttributeAsciidocBuilder builder;

    public RestDocMarkupBuilder(Tree tree, String path) {
        this.tree = tree;
        this.path = path;
        builder = AttributeAsciidocBuilder.newInstance();
    }


    public void build() {
        builder.documentTitle(tree.getInfo().getName())
                .paragraph(tree.getInfo().getDescription());

        for (int i = 0; i < tree.getNodes().size(); i++) {
            Node node = tree.getNodes().get(i);
            buildNode(node, "", i + 1);
        }

        Path indexFile = Paths.get(path + "/index");
        builder.writeToFile(indexFile, StandardCharsets.UTF_8);
    }

    private void buildNode(Node node, String prefix, int num) {
        if (node instanceof Group) {
            buildGroup((Group) node, prefix, num);
        } else if (node instanceof Item) {
            buildItem((Item) node, prefix, num);
        }
    }

    private void buildGroup(Group group, String prefix, int num) {
        builder.sectionTitleLevel1(prefix + num + " " + group.getName());
        if (Objects.nonNull(group.getDescription())) {
            builder.paragraph(group.getDescription(),true);
        }
        for (int i = 0; i < group.getNodes().size(); i++) {
            Node node = group.getNodes().get(i);
            buildNode(node, prefix + num + ".", i + 1);
        }
    }

    private void buildItem(Item item, String prefix, int num) {
        builder.sectionTitleLevel2(prefix + num + " " + item.getName());
        if (Objects.nonNull(item.getDescription())) {
            builder.paragraph(item.getDescription(),true);
        }

        Item.Request request = item.getRequest();
        if (Objects.nonNull(request.getDescription())) {
            builder.paragraph(request.getDescription(),true);
        }
        builder.block(builder -> {
            builder.textLine(request.getMethod().toString()
                    + " "
                    + request.getUrl().getPath() + request.getUrl().getQueryString()
                    + " HTTP/1.1");
            request.getHeader().forEach(header -> builder.textLine(header.toString()));
            builder.newLine();
            builder.textLine(request.getBody().toString());
        }, "REQUEST");

        if (request.getUrl().getQuery().size() > 0
                || request.getUrl().getPathVariable().size() > 0
                || request.getBody().getRawParameter().size() > 0
                || request.getBody().getFormdata().size() > 0
                || request.getBody().getUrlencoded().size() > 0) {
            List<List<String>> requestTable = new ArrayList<>();
            requestTable.add(Arrays.asList("NAME", "TYPE", "DEFAULT", "DESCRIPTION"));
            request.getUrl().getPathVariable().forEach(parameter -> requestTable.add(toList(parameter)));
            request.getUrl().getQuery().forEach(parameter -> requestTable.add(toList(parameter)));
            request.getBody().getUrlencoded().forEach(parameter -> requestTable.add(toList(parameter)));
            request.getBody().getFormdata().forEach(parameter -> requestTable.add(toList(parameter)));
            request.getBody().getRawParameter().forEach(parameter -> requestTable.add(toList(parameter)));
            builder.table(requestTable);
        }

        Item.Response response = item.getResponse();
        if (!response.isEmpty()) {
            if (response.getHeader().size() > 0) {
                builder.block(builder -> {
                    builder.textLine("HTTP/1.1 200 OK");
                    response.getHeader().forEach(header -> builder.textLine(header.toString()));
                    builder.newLine();
                    builder.textLine(response.getBody());
                }, "RESPONSE");
            }
            if (response.getBodyParameter().size() > 0) {
                List<List<String>> responseTable = new ArrayList<>();
                responseTable.add(Arrays.asList("NAME", "TYPE", "DEFAULT", "DESCRIPTION"));
                response.getBodyParameter().forEach(parameter -> responseTable.add(toList(parameter)));
                builder.table(responseTable);
            }

        }

    }

    private List<String> toList(Item.Parameter parameter) {
        return Arrays.asList(parameter.getKey(), parameter.getType(), String.valueOf(parameter.getValue()), parameter.getDescription());
    }

}
