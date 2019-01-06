# Title     : TODO
# Objective : TODO
# Created by: per
# Created on: 1/5/19

library(hamcrest)

test.loop <- function() {
    j = 0
    for (i in 1:25) {
      j <- j + (i^2)
    }
    assertThat(j, identicalTo(5524))
}

test.checkNumbers <- function() {
    assertThat(12, identicalTo(13))
}