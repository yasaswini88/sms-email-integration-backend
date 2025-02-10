package com.example.sms_email_integration.dto;

public class NewCaseCheckDto {
    
    private String caseType;
    private Long caseId;

    public NewCaseCheckDto() {}
    public NewCaseCheckDto(Long caseId, String caseType) {
       
       this.caseId = caseId;
        this.caseType = caseType;
        
    }

  
    public String getCaseType()
     { 
        return caseType; 
     }
    public void setCaseType(String caseType)
     { 
        this.caseType = caseType; 
     }

    public Long getCaseId() {
       
         return caseId; 

         }
    public void setCaseId(Long caseId)
     {
         this.caseId = caseId; 
         
    }
}
