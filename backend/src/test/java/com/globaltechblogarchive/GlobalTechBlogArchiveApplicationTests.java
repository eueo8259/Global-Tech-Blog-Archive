package com.globaltechblogarchive;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class GlobalTechBlogArchiveApplicationTests {

    @Test
    void applicationClassExists() {
        assertDoesNotThrow(() -> Class.forName(GlobalTechBlogArchiveApplication.class.getName()));
    }
}
