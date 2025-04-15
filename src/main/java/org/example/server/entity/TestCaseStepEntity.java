package org.example.server.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "test_case_steps")
public class TestCaseStepEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id")
    private TestCaseEntity testCase;
    
    private int stepOrder;
    private String stepType;
    private String content;
}
