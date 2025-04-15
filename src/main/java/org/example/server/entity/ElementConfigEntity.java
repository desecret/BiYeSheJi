package org.example.server.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "element_configs")
public class ElementConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String context;

    @Lob
    @Column(columnDefinition = "MEDIUMBLOB")
    private byte[] imageData;

    private String imagePath;
    private String xpath;
    private String cssSelector;
    private String locatorType = "image";

}