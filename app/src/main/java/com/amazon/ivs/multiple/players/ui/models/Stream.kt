package com.amazon.ivs.multiple.players.ui.models

enum class FirstLayoutStream(val index: Int, val maxQuality: String, val uri: String) {
    STREAM_A(0, "1080p", "https://4c62a87c1810.us-west-2.playback.live-video.net/api/video/v1/us-west-2.049054135175.channel.QidZjoGOhfDp.m3u8"),
    STREAM_B(1, "480p", "https://4c62a87c1810.us-west-2.playback.live-video.net/api/video/v1/us-west-2.049054135175.channel.LaSuL3bHBRR7.m3u8"),
    STREAM_C(2, "480p", "https://4c62a87c1810.us-west-2.playback.live-video.net/api/video/v1/us-west-2.049054135175.channel.rqyuAWXUrvUS.m3u8")
}

enum class SecondLayoutStream(val index: Int, val maxQuality: String, val uri: String) {
    STREAM_A(0, "1080p", "https://4c62a87c1810.us-west-2.playback.live-video.net/api/video/v1/us-west-2.049054135175.channel.FMaC7IMoyDEA.m3u8"),
    STREAM_B(1, "1080p", "https://4c62a87c1810.us-west-2.playback.live-video.net/api/video/v1/us-west-2.049054135175.channel.WP4bWqiALo67.m3u8")
}

enum class ThirdLayoutStream(val index: Int, val maxQuality: String, val uri: String) {
    STREAM_A(0, "1080p", "https://4c62a87c1810.us-west-2.playback.live-video.net/api/video/v1/us-west-2.049054135175.channel.HPz5Ug1fjNTO.m3u8"),
    STREAM_B(1, "480p", "https://4c62a87c1810.us-west-2.playback.live-video.net/api/video/v1/us-west-2.049054135175.channel.iNMK0w9JnUkC.m3u8")
}
