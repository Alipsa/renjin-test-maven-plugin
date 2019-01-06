# Title     : TODO
# Objective : TODO
# Created by: per
# Created on: 1/6/19
library(hamcrest)

test.somethingSuccessful <- function() {
    assertThat(12.104, identicalTo(12.104))
}


assertThat(mean(cars[,2]), equalTo(23.1))
