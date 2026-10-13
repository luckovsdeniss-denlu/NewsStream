package com.newsstream.data

// Bundled sample content until a live news feed is wired up. The text is an
// original summary of a publicly reported story, credited to its source.
object SampleArticles {
    val all: List<Article> = listOf(
        Article(
            id = "voyager1-interstellar",
            title = "Voyager 1 becomes the first human-made object in interstellar space",
            summary = "NASA confirms its 36-year-old probe has left the Sun's bubble of " +
                "plasma and is now travelling between the stars.",
            body = listOf(
                "NASA announced that its Voyager 1 spacecraft has become the first " +
                    "human-made object to reach interstellar space. Launched on " +
                    "September 5, 1977, the probe is now about 19 billion kilometres " +
                    "(12 billion miles) from the Sun.",
                "Scientists concluded that Voyager 1 crossed the heliopause, the " +
                    "boundary where the solar wind gives way to the material between " +
                    "the stars, around August 25, 2012.",
                "The key evidence came from the spacecraft's plasma wave instrument. " +
                    "When an outburst from the Sun reached Voyager in 2013, it set the " +
                    "surrounding plasma vibrating, and the frequency of those " +
                    "oscillations showed the plasma was roughly 40 times denser than " +
                    "inside the heliosphere — the density expected in interstellar space.",
                "The findings were published in the journal Science, in a paper led by " +
                    "Don Gurnett of the University of Iowa. Voyager 1 continues to send " +
                    "data back to Earth as it travels onward.",
            ),
            source = "NASA",
            sourceUrl = "https://science.nasa.gov/mission/voyager/",
            publishedDate = "September 12, 2013",
        ),
    )
}
