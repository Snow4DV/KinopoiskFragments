package ru.snowadv.kinopoiskfeaturedmovies.presentation.ui.film.info

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.OverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPositionInLayout
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import ru.snowadv.kinopoiskfeaturedmovies.R
import ru.snowadv.kinopoiskfeaturedmovies.domain.model.FilmInfo
import ru.snowadv.kinopoiskfeaturedmovies.feat.util.SampleData
import ru.snowadv.kinopoiskfeaturedmovies.presentation.ui.common.ErrorMessageBox
import ru.snowadv.kinopoiskfeaturedmovies.presentation.ui.common.getBlankString
import ru.snowadv.kinopoiskfeaturedmovies.presentation.ui.common.shimmerEffect
import ru.snowadv.kinopoiskfeaturedmovies.presentation.ui.theme.KinopoiskFeaturedMoviesTheme
import ru.snowadv.kinopoiskfeaturedmovies.presentation.view_model.FilmInfoViewModel
import kotlin.math.abs
import kotlin.math.max

@Composable
fun FilmInfoScreen(
    modifier: Modifier = Modifier,
    filmInfoViewModel: FilmInfoViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    filmId: State<Long?>
) {

    LaunchedEffect(filmId.value) {
        filmInfoViewModel.loadData(filmId.value)
    }

    BackHandler { // Handle back click by myself to erase film id's state
        onBackClick()
        Log.d("TAG", "FilmInfoScreen: pressed back")
    }

    val state = filmInfoViewModel.state.value

    Box(
        modifier = modifier
    ) {
        if(state.error == null) {
            FilmInfoScreenContent(
                modifier = Modifier.fillMaxSize(),
                filmInfo = state.film,
                onBackClick = onBackClick
            )
        } else {
            if (!state.loading) {
                ErrorMessageBox(
                    modifier = Modifier.fillMaxSize(),
                    errorMessage = state.error,
                    onRefresh = if (state.error != null) {
                        {
                            filmInfoViewModel.loadData(filmId.value)
                        }
                    } else null,
                    defaultStringResId = R.string.choose_film
                )
            }
        }
        if(filmId.value != null) {
            Icon(
                modifier = with(LocalDensity.current) {
                    Modifier
                        .height(29.sp.toDp() + 22.dp)
                        .width(29.sp.toDp() + 10.dp)
                        .padding(top = 22.dp, start = 10.dp)
                        .clickable(onClick = onBackClick)
                },
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilmInfoScreenContent(
    modifier: Modifier = Modifier,
    filmInfo: FilmInfo?,
    onBackClick: () -> Unit

) {
    val configuration = LocalConfiguration.current

    val iconPaddingTop = 22.dp
    val iconPaddingStart = 10.dp

    val minBarAndImageHeight = with(LocalDensity.current) { 29.sp.toDp() + iconPaddingTop * 2 }

    val posterWidth = configuration.screenWidthDp.dp
    val posterHeight = remember(configuration.screenHeightDp, posterWidth) {
        min(
            configuration.screenHeightDp.dp - minBarAndImageHeight,
            (posterWidth.value / 27.0f * 40.0f).toInt().dp
        )
    } // movie poster is 27x40 inches

    val scrollState = rememberLazyListState()

    LaunchedEffect(Unit) {
        scrollState.scrollToItem(1)
    }

    val snapPosition = remember {
        SnapPositionInLayout { layoutSize, itemSize, beforeContentPadding, afterContentPadding, _ ->
            beforeContentPadding
        }
    }
    val snappingLayout = remember(scrollState) {
        SnapLayoutInfoProvider(
            scrollState,
            positionInLayout = snapPosition
        )
    }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)

    val paddingBetweenDescriptionUnits = 7.dp
    val fontSizeTitle = 20.sp
    val fontSizeSecondary = 14.sp

    val pullBarHeight = 15.dp

    val scrollThingShape = remember { RoundedCornerShape(25.dp) }

    val closeToPosterSize = abs(posterHeight.value - posterWidth.value / 27.0 * 40.0) < 30

    val contentScale = remember {
        object : ContentScale {
            override fun computeScaleFactor(srcSize: Size, dstSize: Size): ScaleFactor {
                val widthScale = dstSize.width / srcSize.width
                val heightScale = dstSize.height / srcSize.height
                val maxScaleMultiplied = max(widthScale, heightScale) * 1.3f
                return ScaleFactor(maxScaleMultiplied, maxScaleMultiplied)
            }

        }
    }
    Box(
        modifier = modifier
    ) {
        if (!closeToPosterSize)  {
            SubcomposeAsyncImage(
                model = filmInfo?.posterUrl,
                contentDescription = filmInfo?.nameRu ?: stringResource(R.string.no_name),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                if (painter.state is AsyncImagePainter.State.Success) {
                    Image(
                        contentScale = contentScale,
                        modifier = Modifier.fillMaxSize(),
                        painter = painter,
                        contentDescription = contentDescription,
                        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply {
                            setToScale(
                                redScale = 0.5f,
                                greenScale = 0.5f,
                                blueScale = 0.5f,
                                alphaScale = 1f
                            )
                        })
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shimmerEffect(round = false)
                    )
                }
            }
        }
        if(filmInfo?.posterUrl != null) {
            SubcomposeAsyncImage(
                model = filmInfo.posterUrl,
                contentDescription = filmInfo.nameRu ?: stringResource(R.string.no_name),
                modifier = Modifier
                    .width(posterWidth)
                    .height(posterHeight)
            ) {
                when(painter.state) {
                    is AsyncImagePainter.State.Success -> {
                        Image(
                            modifier = Modifier.fillMaxSize(),
                            contentScale = if (closeToPosterSize) ContentScale.Crop else ContentScale.FillHeight,
                            painter = painter,
                            contentDescription = null
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize().shimmerEffect(round = false)
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().shimmerEffect(round = false)
            )
        }
        Icon( // a little bit hacky and blur is expensive. I would ask designer to reconsider this
            modifier = with(LocalDensity.current) {
                Modifier
                    .height(31.sp.toDp() + iconPaddingTop)
                    .width(31.sp.toDp() + iconPaddingStart * 2)
                    .padding(top = iconPaddingTop, start = iconPaddingStart, end = iconPaddingStart)
                    .offset(x = (0).dp, y = 0.dp)
                    .blur(4.dp)
            },
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = null,
            tint = Color.Black
        )
        Icon(
            modifier = with(LocalDensity.current) {
                Modifier
                    .height(29.sp.toDp() + iconPaddingTop)
                    .width(29.sp.toDp() + iconPaddingStart)
                    .padding(top = iconPaddingTop, start = iconPaddingStart)
                    .clickable(onClick = onBackClick)
            },
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back),
            tint = MaterialTheme.colorScheme.primary
        )
        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = minBarAndImageHeight),
                state = scrollState,
                flingBehavior = flingBehavior
            ) {
                item {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(posterHeight - minBarAndImageHeight - pullBarHeight)
                    )
                }
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .width(65.dp)
                                .height(pullBarHeight)
                                .align(alignment = Alignment.CenterHorizontally)
                                .padding(5.dp)
                                .clip(scrollThingShape)
                                .shadow(elevation = 5.dp, shape = scrollThingShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))


                        )
                        Column(
                            modifier = Modifier
                                .shadow(elevation = 10.dp)
                                .background(color = MaterialTheme.colorScheme.surface)
                                .padding(vertical = 20.dp, horizontal = 35.dp)
                                .fillMaxWidth(),
                            //.height(configuration.screenHeightDp.dp - minBarAndImageHeight),
                            verticalArrangement = Arrangement.spacedBy(
                                paddingBetweenDescriptionUnits
                            )
                        ) {
                            Text(
                                text = filmInfo?.let { filmInfo.nameRu ?: filmInfo.nameEn ?: filmInfo.nameOriginal
                                ?: stringResource(
                                    id = R.string.no_name
                                ) } ?: "",
                                fontSize = fontSizeTitle,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(bottom = 3.dp)
                                    .let {
                                        if (filmInfo == null) it
                                            .shimmerEffect()
                                            .fillMaxWidth(0.8f) else it
                                    }
                            )
                            Text(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .let {
                                        if (filmInfo == null) it
                                            .shimmerEffect()
                                            .fillMaxWidth(1f)
                                            .height(150.dp) else it
                                    },
                                text = if(filmInfo == null) " " else filmInfo.description
                                    ?: stringResource(R.string.no_description),
                                fontSize = fontSizeSecondary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = (fontSizeSecondary.value + 2).sp
                            )
                            if (filmInfo?.genres?.isNotEmpty() == true) {
                                Row {
                                    Text(
                                        text = stringResource(if (filmInfo.genres.size == 1) R.string.genre else R.string.genres),
                                        fontSize = fontSizeSecondary,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = (fontSizeSecondary.value + 2).sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = filmInfo.genres.joinToString(", "),
                                        fontSize = fontSizeSecondary,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = (fontSizeSecondary.value + 2).sp,
                                    )
                                }
                            }
                            if (filmInfo?.countries?.isNotEmpty() == true) {
                                Row {
                                    Text(
                                        text = stringResource(if (filmInfo.countries.size == 1) R.string.country else R.string.countries),
                                        fontSize = fontSizeSecondary,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = (fontSizeSecondary.value + 2).sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = filmInfo.countries.joinToString(", "),
                                        fontSize = fontSizeSecondary,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = (fontSizeSecondary.value + 2).sp,
                                    )
                                }
                            }
                            filmInfo?.year?.let { year ->
                                Row {
                                    Text(
                                        text = stringResource(R.string.prod_year),
                                        fontSize = fontSizeSecondary,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = (fontSizeSecondary.value + 2).sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = year.toString(),
                                        fontSize = fontSizeSecondary,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = (fontSizeSecondary.value + 2).sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun FilmInfoScreenContentPreview() {
    val configuration = LocalConfiguration.current
    KinopoiskFeaturedMoviesTheme {
        Surface(
            modifier = Modifier
                .width(configuration.screenWidthDp.dp)
                .height(configuration.screenHeightDp.dp),
        ) {
            FilmInfoScreenContent(
                modifier = Modifier.fillMaxSize(),
                filmInfo = SampleData.filmInfo,
                onBackClick = {}
            )

        }
    }
}