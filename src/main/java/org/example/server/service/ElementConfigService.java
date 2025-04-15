package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.config.ElementConfig;
import org.example.server.entity.ElementConfigEntity;
import org.example.server.repository.ElementConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ElementConfigService {

    private final ElementConfigRepository elementConfigRepository;

    public List<ElementConfig> getAllElements() {
        return elementConfigRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public ElementConfig getElement(String context, String name) {
        return elementConfigRepository.findByContextAndName(context, name)
                .map(this::convertToDto)
                .orElse(null);
    }

    @Transactional
    public ElementConfig createElement(ElementConfig config, MultipartFile image) throws IOException {
        ElementConfigEntity entity = new ElementConfigEntity();
        entity.setName(config.getName());
        entity.setContext(config.getContext());
        entity.setXpath(config.getXpath());
        entity.setCssSelector(config.getCssSelector());
        entity.setLocatorType(config.getLocatorType());
        entity.setImagePath(config.getImagePath());

        if (image != null && !image.isEmpty()) {
            entity.setImageData(image.getBytes());
        }

        ElementConfigEntity saved = elementConfigRepository.save(entity);
        return convertToDto(saved);
    }

    // 添加更新、删除等方法...

    private ElementConfig convertToDto(ElementConfigEntity entity) {
        ElementConfig dto = new ElementConfig();
        dto.setName(entity.getName());
        dto.setContext(entity.getContext());
        dto.setXpath(entity.getXpath());
        dto.setCssSelector(entity.getCssSelector());
        dto.setLocatorType(entity.getLocatorType());
        dto.setImagePath(entity.getImagePath());
        return dto;
    }
}