package com.controller;

import com.dto.Segment;
import com.dto.request.RenameSegmentRequest;
import com.dto.response.RenameSegmentResponse;
import com.dto.response.SegmentUsageResponse;
import com.service.SegmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.FileNotFoundException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// FM-19 follow-up (project-lead feedback on PR #30): HTTP-level coverage for the three new/changed
// Segment endpoints - usage check, cascading rename, and the now-checked delete. Complements
// SegmentServiceTest's unit-level coverage of the actual cascade logic.
@WebMvcTest(SegmentController.class)
class SegmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SegmentService segmentService;

    // ---- GET /segments/segment/{id}/usage ----

    @Test
    void usageEndpointReturns200WithTransactionCount() throws Exception {
        when(segmentService.getSegmentUsage(7)).thenReturn(new SegmentUsageResponse(12));

        mockMvc.perform(get("/segments/segment/7/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCount").value(12));
    }

    @Test
    void usageEndpointReturns404ForUnknownSegmentId() throws Exception {
        when(segmentService.getSegmentUsage(999))
                .thenThrow(new FileNotFoundException("This segment does not exist"));

        mockMvc.perform(get("/segments/segment/999/usage"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("This segment does not exist"));
    }

    // ---- PATCH /segments/segment/{id} ----

    @Test
    void renameEndpointReturns200WithUpdatedSegmentAndCascadeCounts() throws Exception {
        Segment renamed = new Segment("Food");
        renamed.setId(7);
        when(segmentService.renameSegment(eq(7), any(String.class)))
                .thenReturn(new RenameSegmentResponse(renamed, 3, 1));

        mockMvc.perform(patch("/segments/segment/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Food"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.segment.name").value("Food"))
                .andExpect(jsonPath("$.updatedTransactionCount").value(3))
                .andExpect(jsonPath("$.updatedRuleCount").value(1));
    }

    @Test
    void renameEndpointReturns400ForCollisionWithADifferentSegment() throws Exception {
        when(segmentService.renameSegment(eq(7), any(String.class)))
                .thenThrow(new IllegalArgumentException("A different segment named 'Food' already exists"));

        mockMvc.perform(patch("/segments/segment/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Food"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("A different segment named 'Food' already exists"));
    }

    @Test
    void renameEndpointReturns404ForUnknownSegmentId() throws Exception {
        when(segmentService.renameSegment(eq(999), any(String.class)))
                .thenThrow(new FileNotFoundException("This segment does not exist"));

        mockMvc.perform(patch("/segments/segment/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Food"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(content().string("This segment does not exist"));
    }

    // ---- DELETE /segments/segment/{id} ----

    @Test
    void deleteEndpointReturns204OnSuccess() throws Exception {
        mockMvc.perform(delete("/segments/segment/7"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteEndpointReturns404ForUnknownSegmentId() throws Exception {
        org.mockito.Mockito.doThrow(new FileNotFoundException("This segment does not exist"))
                .when(segmentService).deleteSegment(anyInt());

        mockMvc.perform(delete("/segments/segment/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("This segment does not exist"));
    }
}
