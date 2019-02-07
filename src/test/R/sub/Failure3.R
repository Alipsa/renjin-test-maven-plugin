# Title     : TODO
# Objective : TODO
# Created by: per
# Created on: 1/6/19
library(hamcrest)

test.meanCarTravelDistance <- function() {
    meanDist <- mean(cars[,2])
    print(meanDist)
    assertThat(meanDist, equalTo(42.98))
}

test.downloader <- function() {
    library("org.renjin.cran:downloader")

    url <- "http://raw.githubusercontent.com/genomicsclass/dagdata/master/inst/extdata/femaleControlsPopulation.csv"
    filename <- basename(url)
    print(paste("downloading", filename))
    download(url, destfile=filename)
    assertThat(file.exists(filename), identicalTo(TRUE))
    if (fileExists) file.remove(filename)
}



print("# verify that utils download.file works")
url <- "https://raw.githubusercontent.com/genomicsclass/dagdata/master/inst/extdata/femaleControlsPopulation.csv"
filename <- basename(url)
download.file(url, destfile=filename)
fileExists <- file.exists(filename)
assertThat(file.exists(filename), identicalTo(TRUE))
if (fileExists) file.remove(filename)


