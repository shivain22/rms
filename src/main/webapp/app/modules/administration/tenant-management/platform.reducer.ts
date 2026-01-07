import axios from 'axios';
import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { IPlatform } from './platform.model';

const initialState = {
  loading: false,
  errorMessage: null,
  platforms: [] as ReadonlyArray<IPlatform>,
  activePlatforms: [] as ReadonlyArray<IPlatform>,
};

export type PlatformState = Readonly<typeof initialState>;

// Actions
export const getPlatforms = createAsyncThunk('platform/fetch_all', async () => {
  const requestUrl = 'api/platforms';
  return axios.get<IPlatform[]>(requestUrl);
});

export const getActivePlatforms = createAsyncThunk('platform/fetch_active', async () => {
  const requestUrl = 'api/platforms/active';
  return axios.get<IPlatform[]>(requestUrl);
});

export const getPlatform = createAsyncThunk('platform/fetch_one', async (id: string | number) => {
  const requestUrl = `api/platforms/${id}`;
  return axios.get<IPlatform>(requestUrl);
});

export const createPlatform = createAsyncThunk('platform/create', async (platform: IPlatform) => {
  const result = await axios.post<IPlatform>('api/platforms', platform);
  return result;
});

export const updatePlatform = createAsyncThunk('platform/update', async (platform: IPlatform) => {
  const result = await axios.put<IPlatform>(`api/platforms/${platform.id}`, platform);
  return result;
});

export const deletePlatform = createAsyncThunk('platform/delete', async (id: string | number) => {
  const requestUrl = `api/platforms/${id}`;
  await axios.delete(requestUrl);
  return id;
});

export const PlatformSlice = createSlice({
  name: 'platform',
  initialState: initialState as PlatformState,
  reducers: {
    reset() {
      return initialState;
    },
  },
  extraReducers(builder) {
    builder
      .addCase(getPlatforms.pending, state => {
        state.loading = true;
      })
      .addCase(getPlatforms.fulfilled, (state, action) => {
        state.loading = false;
        const platforms = action.payload.data;
        state.platforms = platforms;
        state.activePlatforms = platforms.filter(p => p.active !== false);
      })
      .addCase(getPlatforms.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getActivePlatforms.pending, state => {
        state.loading = true;
      })
      .addCase(getActivePlatforms.fulfilled, (state, action) => {
        state.loading = false;
        const platforms = action.payload.data;
        state.activePlatforms = platforms;
        // Update full list if needed
        platforms.forEach(platform => {
          const index = state.platforms.findIndex(p => p.id === platform.id);
          if (index >= 0) {
            state.platforms[index] = platform;
          } else {
            state.platforms = [...state.platforms, platform];
          }
        });
      })
      .addCase(getActivePlatforms.rejected, (state, action) => {
        state.loading = false;
        state.errorMessage = action.error.message;
      })
      .addCase(getPlatform.fulfilled, (state, action) => {
        state.loading = false;
        const platform = action.payload.data;
        const index = state.platforms.findIndex(p => p.id === platform.id);
        if (index >= 0) {
          state.platforms[index] = platform;
        } else {
          state.platforms = [...state.platforms, platform];
        }
        if (platform.active !== false) {
          const activeIndex = state.activePlatforms.findIndex(p => p.id === platform.id);
          if (activeIndex >= 0) {
            state.activePlatforms[activeIndex] = platform;
          } else {
            state.activePlatforms = [...state.activePlatforms, platform];
          }
        }
      })
      .addCase(createPlatform.fulfilled, (state, action) => {
        state.loading = false;
        const platform = action.payload.data;
        state.platforms = [...state.platforms, platform];
        if (platform.active !== false) {
          state.activePlatforms = [...state.activePlatforms, platform];
        }
      })
      .addCase(updatePlatform.fulfilled, (state, action) => {
        state.loading = false;
        const platform = action.payload.data;
        const index = state.platforms.findIndex(p => p.id === platform.id);
        if (index >= 0) {
          state.platforms[index] = platform;
        }
        if (platform.active !== false) {
          const activeIndex = state.activePlatforms.findIndex(p => p.id === platform.id);
          if (activeIndex >= 0) {
            state.activePlatforms[activeIndex] = platform;
          } else {
            state.activePlatforms = [...state.activePlatforms, platform];
          }
        } else {
          state.activePlatforms = state.activePlatforms.filter(p => p.id !== platform.id);
        }
      })
      .addCase(deletePlatform.fulfilled, (state, action) => {
        state.loading = false;
        const id = action.payload;
        state.platforms = state.platforms.filter(p => p.id !== id);
        state.activePlatforms = state.activePlatforms.filter(p => p.id !== id);
      });
  },
});

export const { reset } = PlatformSlice.actions;

export default PlatformSlice.reducer;
